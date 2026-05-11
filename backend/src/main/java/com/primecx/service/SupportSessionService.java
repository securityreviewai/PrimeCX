package com.primecx.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CreateSessionRequest;
import com.primecx.dto.SupportSessionDto;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.SessionStatus;
import com.primecx.model.SupportSession;
import com.primecx.model.Ticket;
import com.primecx.model.User;
import com.primecx.repository.SupportSessionRepository;
import com.primecx.repository.TicketRepository;
import com.primecx.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportSessionService {

    private final SupportSessionRepository supportSessionRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SupportSession startSession(CreateSessionRequest request, Long executiveId) {
        Ticket ticket = ticketRepository.findById(request.ticketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", request.ticketId()));
        User executive = userRepository.findById(executiveId)
                .orElseThrow(() -> new ResourceNotFoundException("User", executiveId));
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));

        SupportSession session = new SupportSession();
        session.setTicket(ticket);
        session.setSupportExecutive(executive);
        session.setUser(user);
        session.setStatus(SessionStatus.ACTIVE);
        session.setStartTime(LocalDateTime.now());

        log.info("Starting support session for ticket {} by executive {}", request.ticketId(), executiveId);
        return supportSessionRepository.save(session);
    }

    @Transactional
    public SupportSession endSession(Long sessionId, String notes, Long actorUserId) {
        SupportSession session = getSessionById(sessionId);
        session.setStatus(SessionStatus.COMPLETED);
        session.setEndTime(LocalDateTime.now());
        session.setNotes(notes);

        log.info("Ending session {}", sessionId);
        SupportSession saved = supportSessionRepository.save(session);
        Long ticketId = session.getTicket() != null ? session.getTicket().getId() : null;
        auditLogService.appendSessionEnded(actorUserId, saved.getId(), ticketId);
        return saved;
    }

    @Transactional
    public SupportSession cancelSession(Long sessionId) {
        SupportSession session = getSessionById(sessionId);
        session.setStatus(SessionStatus.CANCELLED);
        session.setEndTime(LocalDateTime.now());

        log.info("Cancelling session {}", sessionId);
        return supportSessionRepository.save(session);
    }

    public SupportSession getSessionById(Long id) {
        return supportSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SupportSession", id));
    }

    public List<SupportSession> getSessionsByExecutive(Long executiveId) {
        return supportSessionRepository.findBySupportExecutiveId(executiveId);
    }

    public List<SupportSession> getSessionsByUser(Long userId) {
        return supportSessionRepository.findByUserId(userId);
    }

    public List<SupportSession> getActiveSessions() {
        return supportSessionRepository.findByStatus(SessionStatus.ACTIVE);
    }

    public SupportSessionDto toDto(SupportSession session) {
        String executiveName = session.getSupportExecutive().getFirstName() + " " + session.getSupportExecutive().getLastName();
        String userName = session.getUser().getFirstName() + " " + session.getUser().getLastName();

        return new SupportSessionDto(
                session.getId(),
                session.getTicket().getId(),
                session.getTicket().getTitle(),
                session.getSupportExecutive().getId(),
                executiveName,
                session.getUser().getId(),
                userName,
                session.getStatus(),
                session.getStartTime(),
                session.getEndTime(),
                session.getNotes()
        );
    }
}
