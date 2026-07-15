package com.turnero.repo;

import com.turnero.domain.ServicePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ServicePointRepo extends JpaRepository<ServicePoint, Long> {
  List<ServicePoint> findByTenantIdAndBranchIdOrderById(Long tenantId, Long branchId);
  Optional<ServicePoint> findByIdAndTenantId(Long id, Long tenantId);
}
