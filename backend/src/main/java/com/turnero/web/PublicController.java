package com.turnero.web;

import com.turnero.domain.Branch;
import com.turnero.domain.Ticket;
import com.turnero.repo.BranchRepo;
import com.turnero.repo.TicketRepo;
import com.turnero.service.QueueService;
import com.turnero.sse.SseHub;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.HttpStatus;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PublicController {
  private final BranchRepo branches;
  private final TicketRepo tickets;
  private final QueueService queue;
  private final SseHub sse;

  public PublicController(BranchRepo branches, TicketRepo tickets, QueueService queue, SseHub sse) {
    this.branches = branches; this.tickets = tickets; this.queue = queue; this.sse = sse;
  }

  @GetMapping("/public/display/{branchId}")
  public Map<String, Object> display(@PathVariable Long branchId, @RequestParam String key) {
    Branch b = requireDisplay(branchId, key);
    return queue.displaySnapshot(b.getTenantId(), b.getId());
  }

  @GetMapping("/stream/display/{branchId}")
  public SseEmitter displayStream(@PathVariable Long branchId, @RequestParam String key) {
    requireDisplay(branchId, key);
    return sse.subscribe("display:" + branchId);
  }

  @GetMapping("/public/ticket/{token}")
  public Map<String, Object> ticket(@PathVariable String token) {
    Ticket t = tickets.findByPublicToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
    return queue.ticketStatus(t);
  }

  @GetMapping("/stream/ticket/{token}")
  public SseEmitter ticketStream(@PathVariable String token) {
    tickets.findByPublicToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));
    return sse.subscribe("ticket:" + token);
  }

  private Branch requireDisplay(Long branchId, String key) {
    Branch b = branches.findById(branchId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada"));
    if (!b.getDisplayKey().equals(key))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clave de pantalla inválida");
    return b;
  }
}
