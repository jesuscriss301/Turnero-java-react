package com.turnero.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.Instant;

@Entity @Getter @Setter
public class TicketEvent {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private Long tenantId;
  private Long ticketId;
  private String type;
  private Instant createdAt = Instant.now();

  public static TicketEvent of(Long tenantId, Long ticketId, String type) {
    TicketEvent e = new TicketEvent();
    e.tenantId = tenantId; e.ticketId = ticketId; e.type = type;
    return e;
  }
}
