package com.turnero.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

@Entity @Getter @Setter
public class ServiceDef {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private Long tenantId;
  private Long branchId;
  private String name;
  private String prefix;
  private boolean priorityAllowed;
  private boolean active = true;
}
