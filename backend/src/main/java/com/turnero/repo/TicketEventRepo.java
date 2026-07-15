package com.turnero.repo;

import com.turnero.domain.TicketEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketEventRepo extends JpaRepository<TicketEvent, Long> {}
