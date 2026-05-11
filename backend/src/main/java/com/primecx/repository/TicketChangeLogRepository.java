package com.primecx.repository;

import com.primecx.model.TicketChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TicketChangeLogRepository extends JpaRepository<TicketChangeLog, Long> {

    @Query("""
            SELECT l FROM TicketChangeLog l
            JOIN FETCH l.changedBy
            WHERE l.ticket.id = :ticketId
            ORDER BY l.changedAt DESC
            """)
    List<TicketChangeLog> findByTicketIdWithActorOrderByChangedAtDesc(@Param("ticketId") Long ticketId);

    @Query("""
            SELECT l FROM TicketChangeLog l
            JOIN FETCH l.ticket t
            JOIN FETCH l.changedBy
            WHERE t.id IN :ticketIds AND l.fieldName = 'STATUS'
            ORDER BY t.id ASC, l.changedAt ASC
            """)
    List<TicketChangeLog> findStatusChangesForTickets(@Param("ticketIds") Collection<Long> ticketIds);
}
