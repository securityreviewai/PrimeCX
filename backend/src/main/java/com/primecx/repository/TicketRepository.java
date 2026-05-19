package com.primecx.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.primecx.model.Ticket;
import com.primecx.model.TicketStatus;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    /**
     * Distinct tags from tickets visible to the viewer (same rules as {@link com.primecx.specification.TicketSpecifications#visibleToUser}).
     */
    @Query(value = """
            SELECT DISTINCT LOWER(TRIM(tt.tag)) AS tag
            FROM ticket_tags tt
            INNER JOIN tickets t ON t.id = tt.ticket_id
            WHERE (:role IN ('ROLE_SUPPORT_ADMIN', 'ROLE_SUPPORT_MANAGER'))
               OR (:role = 'ROLE_SUPPORT_EXECUTIVE' AND t.assigned_to_id = :userId)
               OR (:role = 'ROLE_USER' AND t.user_id = :userId)
            ORDER BY tag
            """, nativeQuery = true)
    List<String> findDistinctTagsVisibleTo(@Param("role") String role, @Param("userId") Long userId);

    List<Ticket> findByUserId(Long userId);

    List<Ticket> findByAssignedToId(Long assignedToId);

    long countByAssignedTo_IdAndStatusIn(Long assignedToId, Collection<TicketStatus> statuses);

    List<Ticket> findByStatus(TicketStatus status);

    long countByStatus(TicketStatus status);

    long countByCustomerRatingIsNotNull();

    long countByCustomerRating(Integer rating);

    @Query("SELECT AVG(t.customerRating) FROM Ticket t WHERE t.customerRating IS NOT NULL")
    Double averageCustomerRating();

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.customerFeedback IS NOT NULL AND TRIM(t.customerFeedback) <> ''")
    long countTicketsWithNonEmptyFeedback();
}
