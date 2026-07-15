package com.turnero.web;

import com.turnero.domain.Ticket;
import com.turnero.service.QueueService;
import com.turnero.security.AuthPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TicketController {
  private final QueueService queue;

  public TicketController(QueueService queue) { this.queue = queue; }

  public record IssueReq(Long serviceId, String visitorName, boolean priority) {}

  @PostMapping("/branches/{branchId}/tickets")
  public Map<String, Object> issue(@AuthenticationPrincipal AuthPrincipal p, @PathVariable Long branchId,
                                   @RequestBody IssueReq req) {
    Ticket t = queue.issue(p.tenantId(), branchId, req.serviceId(), req.visitorName(), req.priority());
    return ticketDto(t);
  }

  @PostMapping("/points/{pointId}/next")
  public ResponseEntity<Map<String, Object>> next(@AuthenticationPrincipal AuthPrincipal p, @PathVariable Long pointId) {
    return queue.callNext(p.tenantId(), pointId)
        .map(t -> ResponseEntity.ok(ticketDto(t)))
        .orElse(ResponseEntity.noContent().build());
  }

  @PostMapping("/tickets/{id}/{action}")
  public Map<String, Object> action(@AuthenticationPrincipal AuthPrincipal p,
                                    @PathVariable Long id, @PathVariable String action) {
    return ticketDto(queue.action(p.tenantId(), id, action));
  }

  private Map<String, Object> ticketDto(Ticket t) {
    var m = new java.util.HashMap<String, Object>();
    m.put("id", t.getId());
    m.put("code", t.getCode());
    m.put("status", t.getStatus());
    m.put("priority", t.getPriority() == 1);
    m.put("publicToken", t.getPublicToken());
    m.put("visitorName", t.getVisitorName() == null ? "" : t.getVisitorName());
    m.put("serviceId", t.getServiceId());
    return m;
  }
}
