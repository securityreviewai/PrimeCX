package com.primecx.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.primecx.dto.DashboardStats;
import com.primecx.model.SessionStatus;
import com.primecx.model.TicketPriority;
import com.primecx.model.TicketStatus;
import com.primecx.repository.RecordingRepository;
import com.primecx.repository.SupportSessionRepository;
import com.primecx.repository.TicketRepository;
import com.primecx.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final SupportSessionRepository supportSessionRepository;
    private final RecordingRepository recordingRepository;
    private final UserRepository userRepository;

    public DashboardStats getStats() {
        long totalTickets = ticketRepository.count();
        long openTickets = ticketRepository.countByStatus(TicketStatus.OPEN);
        long criticalOpenTickets = ticketRepository.countByStatusAndPriority(TicketStatus.OPEN, TicketPriority.CRITICAL);
        long openEscalatedTickets = ticketRepository.countByEscalatedTrueAndStatus(TicketStatus.OPEN);
        long openTicketsPastFollowUpDue = ticketRepository.countByStatusInAndFollowUpDueAtBefore(
                List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS), LocalDateTime.now());
        long activeSessions = supportSessionRepository.countByStatus(SessionStatus.ACTIVE);
        long totalRecordings = recordingRepository.count();
        long totalUsers = userRepository.count();

        return new DashboardStats(
                totalTickets,
                openTickets,
                criticalOpenTickets,
                openEscalatedTickets,
                openTicketsPastFollowUpDue,
                activeSessions,
                totalRecordings,
                totalUsers);
    }
}
