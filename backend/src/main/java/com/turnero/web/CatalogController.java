package com.turnero.web;

import com.turnero.domain.*;
import com.turnero.repo.*;
import com.turnero.security.AuthPrincipal;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CatalogController {
  private final BranchRepo branches;
  private final ServiceDefRepo services;
  private final ServicePointRepo points;
  private final TenantRepo tenants;
  private static final SecureRandom RNG = new SecureRandom();

  public CatalogController(BranchRepo branches, ServiceDefRepo services, ServicePointRepo points, TenantRepo tenants) {
    this.branches = branches; this.services = services; this.points = points; this.tenants = tenants;
  }

  // ---- Branches ----
  public record BranchReq(@NotBlank String name) {}

  @GetMapping("/branches")
  public List<Map<String, Object>> listBranches(@AuthenticationPrincipal AuthPrincipal p) {
    return branches.findByTenantIdOrderById(p.tenantId()).stream().map(this::branchDto).toList();
  }

  @PostMapping("/branches")
  public Map<String, Object> createBranch(@AuthenticationPrincipal AuthPrincipal p, @RequestBody BranchReq req) {
    Tenant t = tenants.findById(p.tenantId()).orElseThrow();
    if ("FREE".equals(t.getPlan()) && branches.countByTenantId(p.tenantId()) >= 1)
      throw new IllegalArgumentException("El plan Free permite 1 sucursal. Actualiza tu plan para crear más.");
    Branch b = new Branch();
    b.setTenantId(p.tenantId());
    b.setName(req.name());
    byte[] buf = new byte[24]; RNG.nextBytes(buf);
    b.setDisplayKey(HexFormat.of().formatHex(buf));
    branches.save(b);
    return branchDto(b);
  }

  private Map<String, Object> branchDto(Branch b) {
    return Map.of("id", b.getId(), "name", b.getName(), "displayKey", b.getDisplayKey());
  }

  // ---- Services ----
  public record ServiceReq(@NotBlank String name, @NotBlank String prefix, boolean priorityAllowed) {}

  @GetMapping("/branches/{branchId}/services")
  public List<Map<String, Object>> listServices(@AuthenticationPrincipal AuthPrincipal p, @PathVariable Long branchId) {
    requireBranch(p, branchId);
    return services.findByTenantIdAndBranchIdOrderById(p.tenantId(), branchId).stream()
        .map(this::serviceDto).toList();
  }

  @PostMapping("/branches/{branchId}/services")
  public Map<String, Object> createService(@AuthenticationPrincipal AuthPrincipal p, @PathVariable Long branchId,
                                           @RequestBody ServiceReq req) {
    requireBranch(p, branchId);
    ServiceDef s = new ServiceDef();
    s.setTenantId(p.tenantId());
    s.setBranchId(branchId);
    s.setName(req.name());
    s.setPrefix(req.prefix().toUpperCase().substring(0, Math.min(3, req.prefix().length())));
    s.setPriorityAllowed(req.priorityAllowed());
    services.save(s);
    return serviceDto(s);
  }

  @PatchMapping("/services/{id}/toggle")
  public Map<String, Object> toggleService(@AuthenticationPrincipal AuthPrincipal p, @PathVariable Long id) {
    ServiceDef s = services.findByIdAndTenantId(id, p.tenantId())
        .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado"));
    s.setActive(!s.isActive());
    services.save(s);
    return serviceDto(s);
  }

  private Map<String, Object> serviceDto(ServiceDef s) {
    return Map.of("id", s.getId(), "name", s.getName(), "prefix", s.getPrefix(),
        "priorityAllowed", s.isPriorityAllowed(), "active", s.isActive());
  }

  // ---- Points ----
  public record PointReq(@NotBlank String name, List<Long> serviceIds) {}

  @GetMapping("/branches/{branchId}/points")
  public List<Map<String, Object>> listPoints(@AuthenticationPrincipal AuthPrincipal p, @PathVariable Long branchId) {
    requireBranch(p, branchId);
    return points.findByTenantIdAndBranchIdOrderById(p.tenantId(), branchId).stream()
        .map(this::pointDto).toList();
  }

  @PostMapping("/branches/{branchId}/points")
  public Map<String, Object> createPoint(@AuthenticationPrincipal AuthPrincipal p, @PathVariable Long branchId,
                                         @RequestBody PointReq req) {
    requireBranch(p, branchId);
    ServicePoint sp = new ServicePoint();
    sp.setTenantId(p.tenantId());
    sp.setBranchId(branchId);
    sp.setName(req.name());
    sp.setServices(resolveServices(p.tenantId(), branchId, req.serviceIds()));
    points.save(sp);
    return pointDto(sp);
  }

  @PutMapping("/points/{id}")
  public Map<String, Object> updatePoint(@AuthenticationPrincipal AuthPrincipal p, @PathVariable Long id,
                                         @RequestBody PointReq req) {
    ServicePoint sp = points.findByIdAndTenantId(id, p.tenantId())
        .orElseThrow(() -> new IllegalArgumentException("Punto no encontrado"));
    sp.setName(req.name());
    sp.setServices(resolveServices(p.tenantId(), sp.getBranchId(), req.serviceIds()));
    points.save(sp);
    return pointDto(sp);
  }

  private Set<ServiceDef> resolveServices(Long tenantId, Long branchId, List<Long> ids) {
    if (ids == null) return new HashSet<>();
    return ids.stream()
        .map(id -> services.findByIdAndTenantId(id, tenantId)
            .filter(s -> s.getBranchId().equals(branchId))
            .orElseThrow(() -> new IllegalArgumentException("Servicio inválido: " + id)))
        .collect(Collectors.toCollection(HashSet::new));
  }

  private Map<String, Object> pointDto(ServicePoint sp) {
    return Map.of("id", sp.getId(), "name", sp.getName(), "status", sp.getStatus(),
        "serviceIds", sp.getServices().stream().map(ServiceDef::getId).sorted().toList());
  }

  private void requireBranch(AuthPrincipal p, Long branchId) {
    branches.findByIdAndTenantId(branchId, p.tenantId())
        .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada"));
  }
}
