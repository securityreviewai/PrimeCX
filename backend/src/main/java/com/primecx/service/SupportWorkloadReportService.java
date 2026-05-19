package com.primecx.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.ExecutiveWorkloadDto;
import com.primecx.model.Role;
import com.primecx.model.SessionStatus;
import com.primecx.model.TicketStatus;
import com.primecx.model.User;
import com.primecx.repository.SupportSessionRepository;
import com.primecx.repository.TicketRepository;
import com.primecx.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupportWorkloadReportService {

    private static final List<TicketStatus> QUEUE_STATUSES = List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final SupportSessionRepository supportSessionRepository;

    @Transactional(readOnly = true)
    public List<ExecutiveWorkloadDto> executiveWorkloadSnapshot() {
        return userRepository.findByRole(Role.ROLE_SUPPORT_EXECUTIVE).stream()
                .filter(User::isActive)
                .map(u -> {
                    String name = (u.getFirstName() + " " + u.getLastName()).strip();
                    long tickets = ticketRepository.countByAssignedTo_IdAndStatusIn(u.getId(), QUEUE_STATUSES);
                    long sessions = supportSessionRepository.countBySupportExecutive_IdAndStatus(
                            u.getId(), SessionStatus.ACTIVE);
                    return new ExecutiveWorkloadDto(u.getId(), name, tickets, sessions);
                })
                .sorted(Comparator.comparing(ExecutiveWorkloadDto::executiveName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
