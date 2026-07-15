package com.turnero.repo;

import com.turnero.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BranchRepo extends JpaRepository<Branch, Long> {
  List<Branch> findByTenantIdOrderById(Long tenantId);
  long countByTenantId(Long tenantId);
  Optional<Branch> findByIdAndTenantId(Long id, Long tenantId);
}
