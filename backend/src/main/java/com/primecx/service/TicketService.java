package com.primecx.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CreateTicketRequest;
import com.primecx.dto.TicketDto;
import com.primecx.dto.UpdateTicketRequest;
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

    private static boolean canViewInternalNotes(User viewer) {
        Role r = viewer.getRole();
        return r == Role.ROLE_SUPPORT_ADMIN
                || r == Role.ROLE_SUPPORT_MANAGER
                || r == Role.ROLE_SUPPORT_EXECUTIVE;
    }

    private void assertTicketVisibleToUser(Ticket ticket, User viewer) {
        Role r = viewer.getRole();
        if (r == Role.ROLE_SUPPORT_ADMIN || r == Role.ROLE_SUPPORT_MANAGER) {
            return;
        }
        if (r == Role.ROLE_SUPPORT_EXECUTIVE) {
            if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(viewer.getId())) {
                return;
            }
            throw new ResourceNotFoundException("Ticket", ticket.getId());
        }
        if (ticket.getUser().getId().equals(viewer.getId())) {
            return;
        }
        throw new ResourceNotFoundException("Ticket", ticket.getId());
    }

    @Transactional
    public Ticket createTicket(CreateTicketRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setPriority(request.priority());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setUser(user);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        log.info("Creating ticket '{}' for user {}", request.title(), userId);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket updateTicket(Long ticketId, UpdateTicketRequest request, User currentUser) {
        Ticket ticket = getTicketById(ticketId);
        assertTicketVisibleToUser(ticket, currentUser);

        if (request.title() != null) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.status() != null) {
            ticket.setStatus(request.status());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.assignedToId() != null) {
            User assignee = userRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.assignedToId()));
            ticket.setAssignedTo(assignee);
        }
        if (request.internalNotes() != null && canViewInternalNotes(currentUser)) {
            ticket.setInternalNotes(request.internalNotes());
        }

        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }

    public Ticket getTicketForUser(Long id, User viewer) {
        Ticket ticket = getTicketById(id);
        assertTicketVisibleToUser(ticket, viewer);
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

    public TicketDto toDto(Ticket ticket, User viewer) {
        String userName = ticket.getUser().getFirstName() + " " + ticket.getUser().getLastName();
        String assignedToName = ticket.getAssignedTo() != null
                ? ticket.getAssignedTo().getFirstName() + " " + ticket.getAssignedTo().getLastName()
                : null;
        Long assignedToId = ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null;
        String internalNotes = canViewInternalNotes(viewer) ? ticket.getInternalNotes() : null;

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
                internalNotes,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
