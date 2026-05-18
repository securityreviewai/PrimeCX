package com.primecx.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.primecx.model.TicketMessage;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    List<TicketMessage> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);

    @Query("""
            SELECT m FROM TicketMessage m
            WHERE m.ticket.id = :ticketId
              AND (:includeInternal = TRUE OR m.internalNote = FALSE)
            ORDER BY m.createdAt ASC
            """)
    List<TicketMessage> findVisibleForTicket(@Param("ticketId") Long ticketId,
            @Param("includeInternal") boolean includeInternal);
}
