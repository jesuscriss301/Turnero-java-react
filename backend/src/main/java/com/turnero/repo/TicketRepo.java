package com.turnero.repo;

import com.turnero.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TicketRepo extends JpaRepository<Ticket, Long> {
  Optional<Ticket> findByPublicToken(String token);
  Optional<Ticket> findByIdAndTenantId(Long id, Long tenantId);

  @Query("select t from Ticket t where t.tenantId=:t and t.branchId=:b and t.status='WAITING' order by t.priority desc, t.id asc")
  List<Ticket> waiting(@Param("t") Long tenantId, @Param("b") Long branchId);

  @Query("select t from Ticket t where t.tenantId=:t and t.branchId=:b and t.status in ('CALLED','IN_SERVICE') order by t.calledAt desc limit 6")
  List<Ticket> recentlyCalled(@Param("t") Long tenantId, @Param("b") Long branchId);

  @Query("select count(t) from Ticket t where t.tenantId=:t and t.branchId=:b and t.status='WAITING' and (t.priority > :p or (t.priority = :p and t.id < :id))")
  long positionAhead(@Param("t") Long tenantId, @Param("b") Long branchId, @Param("p") int priority, @Param("id") Long id);
}
