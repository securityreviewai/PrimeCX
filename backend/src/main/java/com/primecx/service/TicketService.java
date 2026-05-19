package com.primecx.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CreateTicketRequest;
import com.primecx.dto.TicketDto;
import com.primecx.dto.UpdateTicketRequest;
import com.primecx.exception.ForbiddenException;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Role;
import com.primecx.model.Ticket;
import com.primecx.model.TicketStatus;
import com.primecx.model.User;
import com.primecx.repository.TicketRepository;
import com.primecx.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SlaService slaService;

    @Transactional
    public Ticket createTicket(CreateTicketRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setPriority(request.priority() != null ? request.priority() : com.primecx.model.TicketPriority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setUser(user);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        slaService.applySlaDeadlines(ticket);

        log.info("Creating ticket '{}' for user {}", request.title(), userId);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket updateTicket(Long ticketId, UpdateTicketRequest request, User currentUser) {
        Ticket ticket = getTicketVisibleToUser(ticketId, currentUser);

        if (request.title() != null) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.status() != null) {
            if (!isStaff(currentUser) && request.status() != ticket.getStatus()) {
                throw new ForbiddenException("Only support staff can change ticket status");
            }
            ticket.setStatus(request.status());
        }
        if (request.priority() != null) {
            if (!isStaff(currentUser)) {
                throw new ForbiddenException("Only support staff can change ticket priority");
            }
            ticket.setPriority(request.priority());
            slaService.applySlaDeadlines(ticket);
        }
        if (request.assignedToId() != null) {
            if (!isStaff(currentUser)) {
                throw new ForbiddenException("Only support staff can assign tickets");
            }
            User assignee = userRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.assignedToId()));
            ticket.setAssignedTo(assignee);
        }

        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }

    public Ticket getTicketVisibleToUser(Long ticketId, User currentUser) {
        Ticket ticket = getTicketById(ticketId);
        if (!canAccessTicket(ticket, currentUser)) {
            throw new ForbiddenException("You do not have access to this ticket");
        }
        return ticket;
    }

    public List<Ticket> getTicketsByUser(Long userId) {
        return ticketRepository.findByUserId(userId);
    }

    public List<Ticket> getTicketsByAssignee(Long assigneeId) {
        return ticketRepository.findByAssignedToId(assigneeId);
    }

    public List<Ticket> getTicketsByStatus(TicketStatus status) {
        return ticketRepository.findByStatus(status);
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public TicketDto toDto(Ticket ticket) {
        String userName = ticket.getUser().getFirstName() + " " + ticket.getUser().getLastName();
        String assignedToName = ticket.getAssignedTo() != null
                ? ticket.getAssignedTo().getFirstName() + " " + ticket.getAssignedTo().getLastName()
                : null;
        Long assignedToId = ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null;

        return new TicketDto(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getUser().getId(),
                userName,
                assignedToId,
                assignedToName,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getSlaRespondBy(),
                ticket.getSlaResolveBy(),
                ticket.getFirstRespondedAt(),
                slaService.isResponseBreached(ticket),
                slaService.isResolveBreached(ticket),
                slaService.isResponseAtRisk(ticket),
                slaService.isResolveAtRisk(ticket)
        );
    }

    private boolean canAccessTicket(Ticket ticket, User user) {
        if (isStaff(user)) {
            return true;
        }
        return ticket.getUser().getId().equals(user.getId());
    }

    private boolean isStaff(User user) {
        Role role = user.getRole();
        return role == Role.ROLE_SUPPORT_EXECUTIVE
                || role == Role.ROLE_SUPPORT_ADMIN
                || role == Role.ROLE_SUPPORT_MANAGER;
    }
}
