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
import com.primecx.model.TicketCategory;
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
        ticket.setCategory(request.category() != null ? request.category() : TicketCategory.GENERAL);
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
        if (request.escalated() != null && canViewInternalNotes(currentUser)) {
            ticket.setEscalated(request.escalated());
        }
        if (request.supportReply() != null && canViewInternalNotes(currentUser)) {
            ticket.setSupportReply(request.supportReply());
        }
        if (request.category() != null && canViewInternalNotes(currentUser)) {
            ticket.setCategory(request.category());
        }

        if (Boolean.TRUE.equals(request.clearFollowUpDueAt()) && canViewInternalNotes(currentUser)) {
            ticket.setFollowUpDueAt(null);
        } else if (request.followUpDueAt() != null && canViewInternalNotes(currentUser)) {
            ticket.setFollowUpDueAt(request.followUpDueAt());
        }

        if (request.satisfactionRating() != null
                || (request.satisfactionComment() != null && !request.satisfactionComment().isBlank())) {
            applyCustomerSatisfaction(ticket, currentUser, request.satisfactionRating(), request.satisfactionComment());
        }

        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    private static void applyCustomerSatisfaction(
            Ticket ticket, User viewer, Integer rating, String comment) {
        if (viewer.getRole() != Role.ROLE_USER) {
            throw new IllegalArgumentException("Only customers may submit satisfaction feedback");
        }
        if (!viewer.getId().equals(ticket.getUser().getId())) {
            throw new IllegalArgumentException("You can only rate your own tickets");
        }
        if (ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED) {
            throw new IllegalArgumentException("Satisfaction can be submitted only after the ticket is resolved or closed");
        }
        if (ticket.getSatisfactionRating() != null) {
            throw new IllegalArgumentException("Feedback has already been submitted for this ticket");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Satisfaction rating must be between 1 and 5");
        }
        ticket.setSatisfactionRating(rating);
        if (comment != null && !comment.isBlank()) {
            ticket.setSatisfactionComment(comment.trim());
        }
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
        boolean escalated = canViewInternalNotes(viewer) && ticket.isEscalated();

        return new TicketDto(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCategory(),
                ticket.getUser().getId(),
                userName,
                assignedToId,
                assignedToName,
                internalNotes,
                ticket.getSupportReply(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                escalated,
                ticket.getFollowUpDueAt(),
                ticket.getSatisfactionRating(),
                ticket.getSatisfactionComment()
        );
    }
}
