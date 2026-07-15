package com.turnero.domain;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.util.HashSet; import java.util.Set;

@Entity @Getter @Setter
public class ServicePoint {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private Long tenantId;
  private Long branchId;
  private String name;
  private String status = "OPEN";
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "point_service",
    joinColumns = @JoinColumn(name = "point_id"),
    inverseJoinColumns = @JoinColumn(name = "service_id"))
  private Set<ServiceDef> services = new HashSet<>();
}
