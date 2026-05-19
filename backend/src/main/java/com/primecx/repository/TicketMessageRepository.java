package com.primecx.repository;

import com.primecx.model.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    List<TicketMessage> findByTicketIdAndInternalNoteFalseOrderByCreatedAtAsc(Long ticketId);

    boolean existsByTicketIdAndAuthorIdNot(Long ticketId, Long authorId);
}
