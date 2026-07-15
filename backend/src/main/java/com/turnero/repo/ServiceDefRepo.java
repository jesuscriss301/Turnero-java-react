package com.turnero.repo;

import com.turnero.domain.ServiceDef;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ServiceDefRepo extends JpaRepository<ServiceDef, Long> {
  List<ServiceDef> findByTenantIdAndBranchIdOrderById(Long tenantId, Long branchId);
  Optional<ServiceDef> findByIdAndTenantId(Long id, Long tenantId);
}
