package com.turnero.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.Instant; import java.time.LocalDate;

@Entity @Getter @Setter
public class Tenant {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private String name;
  private String plan = "FREE";
  private LocalDate freeExpiresAt;
  private Instant createdAt = Instant.now();
}
