package com.primecx.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.primecx.model.TicketActivityLog;

@Repository
public interface TicketActivityLogRepository extends JpaRepository<TicketActivityLog, Long> {

    Page<TicketActivityLog> findByTicket_IdOrderByCreatedAtDesc(Long ticketId, Pageable pageable);
}
