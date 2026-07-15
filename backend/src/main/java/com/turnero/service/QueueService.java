package com.turnero.service;

import com.turnero.domain.*;
import com.turnero.repo.*;
import com.turnero.sse.SseHub;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QueueService {
  private final TicketRepo tickets;
  private final TicketEventRepo events;
  private final ServiceDefRepo services;
  private final ServicePointRepo points;
  private final BranchRepo branches;
  private final TenantRepo tenants;
  private final JdbcTemplate jdbc;
  private final SseHub sse;

  public QueueService(TicketRepo tickets, TicketEventRepo events, ServiceDefRepo services,
                      ServicePointRepo points, BranchRepo branches, TenantRepo tenants,
                      JdbcTemplate jdbc, SseHub sse) {
    this.tickets = tickets; this.events = events; this.services = services;
    this.points = points; this.branches = branches; this.tenants = tenants;
    this.jdbc = jdbc; this.sse = sse;
  }

  @Transactional
  public Ticket issue(Long tenantId, Long branchId, Long serviceId, String visitorName, boolean priority) {
    Tenant tenant = tenants.findById(tenantId).orElseThrow();
    if ("FREE".equals(tenant.getPlan()) && tenant.getFreeExpiresAt() != null
        && tenant.getFreeExpiresAt().isBefore(LocalDate.now())) {
      throw new IllegalArgumentException("El plan gratuito expiró. Actualiza tu plan para seguir emitiendo turnos.");
    }
    Branch branch = branches.findByIdAndTenantId(branchId, tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada"));
    ServiceDef service = services.findByIdAndTenantId(serviceId, tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado"));
    if (!service.isActive() || !service.getBranchId().equals(branch.getId()))
      throw new IllegalArgumentException("Servicio inactivo o de otra sucursal");
    if (priority && !service.isPriorityAllowed())
      throw new IllegalArgumentException("Este servicio no admite prioridad");

    int seq = nextSeq(tenantId, branchId, serviceId);
    Ticket t = new Ticket();
    t.setTenantId(tenantId);
    t.setBranchId(branchId);
    t.setServiceId(serviceId);
    t.setCode(service.getPrefix().toUpperCase() + "-" + String.format("%03d", seq));
    t.setPriority(priority ? 1 : 0);
    t.setPublicToken(UUID.randomUUID().toString().replace("-", "") + Long.toHexString(System.nanoTime()));
    t.setVisitorName(visitorName);
    tickets.save(t);
    events.save(TicketEvent.of(tenantId, t.getId(), "ISSUED"));
    broadcast(tenantId, branchId, t);
    return t;
  }

  private int nextSeq(Long tenantId, Long branchId, Long serviceId) {
    jdbc.update("INSERT INTO daily_counter (tenant_id, branch_id, service_id, day, value) " +
        "VALUES (?,?,?,?, LAST_INSERT_ID(1)) " +
        "ON DUPLICATE KEY UPDATE value = LAST_INSERT_ID(value + 1)",
        tenantId, branchId, serviceId, LocalDate.now());
    Integer v = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    return v == null ? 1 : v;
  }

  @Transactional
  public Optional<Ticket> callNext(Long tenantId, Long pointId) {
    ServicePoint point = points.findByIdAndTenantId(pointId, tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Punto no encontrado"));
    if (point.getServices().isEmpty()) throw new IllegalArgumentException("El punto no tiene servicios compatibles configurados");
    String in = point.getServices().stream().map(s -> String.valueOf(s.getId())).collect(Collectors.joining(","));
    List<Long> ids = jdbc.queryForList(
        "SELECT id FROM ticket WHERE tenant_id=? AND branch_id=? AND status='WAITING' " +
        "AND service_id IN (" + in + ") ORDER BY priority DESC, id ASC LIMIT 1 FOR UPDATE SKIP LOCKED",
        Long.class, tenantId, point.getBranchId());
    if (ids.isEmpty()) return Optional.empty();
    Ticket t = tickets.findById(ids.get(0)).orElseThrow();
    t.setStatus("CALLED");
    t.setPointId(point.getId());
    t.setCalledAt(Instant.now());
    t.setCallCount(t.getCallCount() + 1);
    tickets.save(t);
    events.save(TicketEvent.of(tenantId, t.getId(), "CALLED"));
    broadcast(tenantId, t.getBranchId(), t);
    return Optional.of(t);
  }

  @Transactional
  public Ticket action(Long tenantId, Long ticketId, String action) {
    Ticket t = tickets.findByIdAndTenantId(ticketId, tenantId)
        .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado"));
    switch (action) {
      case "start" -> {
        requireStatus(t, "CALLED");
        t.setStatus("IN_SERVICE"); t.setStartedAt(Instant.now());
      }
      case "finish" -> {
        requireStatus(t, "IN_SERVICE");
        t.setStatus("FINISHED"); t.setFinishedAt(Instant.now());
      }
      case "absent" -> {
        requireStatus(t, "CALLED");
        t.setStatus("ABSENT"); t.setFinishedAt(Instant.now());
      }
      case "recall" -> {
        requireStatus(t, "CALLED");
        t.setCallCount(t.getCallCount() + 1); t.setCalledAt(Instant.now());
      }
      case "cancel" -> {
        requireStatus(t, "WAITING");
        t.setStatus("CANCELLED"); t.setFinishedAt(Instant.now());
      }
      default -> throw new IllegalArgumentException("Acción desconocida");
    }
    tickets.save(t);
    events.save(TicketEvent.of(tenantId, t.getId(), action.toUpperCase()));
    broadcast(tenantId, t.getBranchId(), t);
    return t;
  }

  private void requireStatus(Ticket t, String expected) {
    if (!expected.equals(t.getStatus()))
      throw new IllegalArgumentException("El turno está en estado " + t.getStatus());
  }

  public Map<String, Object> displaySnapshot(Long tenantId, Long branchId) {
    Tenant tenant = tenants.findById(tenantId).orElseThrow();
    Map<Long, String> pointNames = new HashMap<>();
    points.findByTenantIdAndBranchIdOrderById(tenantId, branchId)
        .forEach(p -> pointNames.put(p.getId(), p.getName()));
    Map<Long, String> servicePrefix = new HashMap<>();
    services.findByTenantIdAndBranchIdOrderById(tenantId, branchId)
        .forEach(s -> servicePrefix.put(s.getId(), s.getName()));

    List<Map<String, Object>> called = tickets.recentlyCalled(tenantId, branchId).stream()
        .map(t -> Map.<String, Object>of(
            "code", t.getCode(),
            "point", t.getPointId() != null ? pointNames.getOrDefault(t.getPointId(), "") : "",
            "status", t.getStatus()))
        .toList();
    List<Map<String, Object>> waiting = tickets.waiting(tenantId, branchId).stream()
        .limit(25)
        .map(t -> Map.<String, Object>of(
            "code", t.getCode(),
            "service", servicePrefix.getOrDefault(t.getServiceId(), ""),
            "priority", t.getPriority() == 1))
        .toList();
    Map<String, Object> snap = new HashMap<>();
    snap.put("plan", tenant.getPlan());
    snap.put("tenantName", tenant.getName());
    snap.put("called", called);
    snap.put("waiting", waiting);
    return snap;
  }

  public Map<String, Object> ticketStatus(Ticket t) {
    Map<String, Object> m = new HashMap<>();
    m.put("code", t.getCode());
    m.put("status", t.getStatus());
    m.put("priority", t.getPriority() == 1);
    if ("WAITING".equals(t.getStatus())) {
      m.put("ahead", tickets.positionAhead(t.getTenantId(), t.getBranchId(), t.getPriority(), t.getId()));
    }
    if (t.getPointId() != null) {
      points.findById(t.getPointId()).ifPresent(p -> m.put("point", p.getName()));
    }
    return m;
  }

  private void broadcast(Long tenantId, Long branchId, Ticket changed) {
    sse.send("display:" + branchId, "queue", displaySnapshot(tenantId, branchId));
    sse.send("ticket:" + changed.getPublicToken(), "status", ticketStatus(changed));
  }
}
