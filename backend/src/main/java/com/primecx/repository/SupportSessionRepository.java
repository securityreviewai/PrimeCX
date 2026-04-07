package com.primecx.repository;

import com.primecx.model.SessionStatus;
import com.primecx.model.SupportSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportSessionRepository extends JpaRepository<SupportSession, Long> {

    List<SupportSession> findByTicketId(Long ticketId);

    List<SupportSession> findBySupportExecutiveId(Long supportExecutiveId);

    List<SupportSession> findByUserId(Long userId);

    List<SupportSession> findByStatus(SessionStatus status);

    long countByStatus(SessionStatus status);
}
