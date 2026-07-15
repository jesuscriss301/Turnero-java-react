package com.turnero.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.Instant;

@Entity @Getter @Setter
public class AppUser {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private Long tenantId;
  @Column(unique = true) private String email;
  private String passwordHash;
  private String name;
  private String role = "ADMIN";
  private Instant createdAt = Instant.now();
}
