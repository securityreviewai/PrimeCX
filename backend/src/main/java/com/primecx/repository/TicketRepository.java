package com.primecx.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.primecx.model.Ticket;
import com.primecx.model.TicketStatus;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUserId(Long userId);

    List<Ticket> findByAssignedToId(Long assignedToId);

    List<Ticket> findByStatus(TicketStatus status);

    long countByStatus(TicketStatus status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.dueAt IS NOT NULL AND t.dueAt < :now "
            + "AND t.status NOT IN (:resolved, :closed)")
    long countOverdue(
            @Param("now") LocalDateTime now,
            @Param("resolved") TicketStatus resolved,
            @Param("closed") TicketStatus closed);
}
