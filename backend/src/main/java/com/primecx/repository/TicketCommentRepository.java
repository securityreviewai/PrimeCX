package com.primecx.repository;

import com.primecx.model.TicketComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    @EntityGraph(attributePaths = "author")
    List<TicketComment> findByTicket_IdOrderByCreatedAtAsc(Long ticketId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Query("SELECT c FROM TicketComment c WHERE c.ticket.id = :ticketId ORDER BY c.createdAt ASC")
    List<TicketComment> findAllForTicketWithAuthors(@Param("ticketId") Long ticketId);

    @EntityGraph(attributePaths = "author")
    @Query("""
            SELECT c FROM TicketComment c
            WHERE c.ticket.id = :ticketId
            AND (:staff = true OR c.internal = false)
            ORDER BY c.createdAt ASC
            """)
    List<TicketComment> findVisibleForTicket(@Param("ticketId") Long ticketId, @Param("staff") boolean staff);

    @EntityGraph(attributePaths = { "author", "ticket" })
    @Query("SELECT c FROM TicketComment c WHERE c.id = :id AND c.ticket.id = :ticketId")
    Optional<TicketComment> findByIdAndTicket_Id(@Param("id") Long id, @Param("ticketId") Long ticketId);

    @EntityGraph(attributePaths = { "author", "ticket" })
    @Query("""
            SELECT c FROM TicketComment c
            WHERE c.ticket.id IN :ticketIds
            ORDER BY c.ticket.id ASC, c.createdAt ASC
            """)
    List<TicketComment> findAllForTicketsWithAuthors(@Param("ticketIds") Collection<Long> ticketIds);

    @EntityGraph(attributePaths = { "author", "ticket" })
    @Query("""
            SELECT c FROM TicketComment c
            WHERE c.author.id = :authorId
            AND c.ticket.assignedTo.id = :assigneeId
            AND c.internal = false
            AND c.createdAt >= :since
            ORDER BY c.createdAt DESC
            """)
    List<TicketComment> findRecentByAuthorOnAssignedTickets(
            @Param("authorId") Long authorId,
            @Param("assigneeId") Long assigneeId,
            @Param("since") LocalDateTime since);
}
