package com.turnero.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.Instant;

@Entity @Getter @Setter
public class Ticket {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private Long tenantId;
  private Long branchId;
  private Long serviceId;
  private Long pointId;
  private String code;
  private String status = "WAITING";
  @Column(columnDefinition = "TINYINT")
  private int priority;
  private String publicToken;
  private String visitorName;
  private int callCount;
  private Instant createdAt = Instant.now();
  private Instant calledAt;
  private Instant startedAt;
  private Instant finishedAt;
}
