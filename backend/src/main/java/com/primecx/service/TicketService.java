package com.primecx.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CreateTicketRequest;
import com.primecx.dto.TicketDto;
import com.primecx.dto.UpdateTicketRequest;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Role;
import com.primecx.model.Ticket;
import com.primecx.model.TicketPriority;
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

    @Transactional
    public Ticket createTicket(CreateTicketRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setPriority(request.priority() != null ? request.priority() : TicketPriority.MEDIUM);
        ticket.setDueAt(request.dueAt());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setUser(user);
        ticket.setLastUpdatedBy(user);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        log.info("Creating ticket '{}' for user {}", request.title(), userId);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket updateTicket(Long ticketId, UpdateTicketRequest request, Long actorUserId) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", actorUserId));
        Ticket ticket = getTicketById(ticketId);
        requireTicketAccess(actor, ticket);

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
        if (Boolean.TRUE.equals(request.clearDueAt())) {
            ticket.setDueAt(null);
        } else if (request.dueAt() != null) {
            ticket.setDueAt(request.dueAt());
        }

        ticket.setLastUpdatedBy(actor);
        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    /**
     * Updates {@code updatedAt} / {@code lastUpdatedBy} after comment activity so list/detail "last touch"
     * stays aligned with collaboration.
     */
    @Transactional
    public void recordTicketLastTouch(Ticket ticket, User actor) {
        ticket.setLastUpdatedBy(actor);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }

    public Ticket getTicketForViewer(Long id, User viewer) {
        Ticket ticket = getTicketById(id);
        requireTicketAccess(viewer, ticket);
        return ticket;
    }

    /**
     * Customer: own ticket. Executive: assigned ticket. Admin/Manager: any.
     */
    public void requireTicketAccess(User u, Ticket ticket) {
        Role r = u.getRole();
        if (r == Role.ROLE_SUPPORT_ADMIN || r == Role.ROLE_SUPPORT_MANAGER) {
            return;
        }
        if (r == Role.ROLE_SUPPORT_EXECUTIVE) {
            if (ticket.getAssignedTo() != null && Objects.equals(ticket.getAssignedTo().getId(), u.getId())) {
                return;
            }
            throw new AccessDeniedException("Not authorized for this ticket");
        }
        if (Objects.equals(ticket.getUser().getId(), u.getId())) {
            return;
        }
        throw new AccessDeniedException("Not authorized for this ticket");
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

        User touch = ticket.getLastUpdatedBy();
        Long lastUpdatedById = touch != null ? touch.getId() : null;
        String lastUpdatedByName = touch != null ? formatUserDisplayName(touch) : null;

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
                ticket.getDueAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                lastUpdatedById,
                lastUpdatedByName
        );
    }

    private static String formatUserDisplayName(User u) {
        String display = ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                + (u.getLastName() != null ? u.getLastName() : "")).trim();
        if (!display.isEmpty()) {
            return display;
        }
        return u.getEmail() != null ? u.getEmail() : ("user-" + u.getId());
    }

    /**
     * SLA overdue: has a due time, not resolved/closed, and due time is in the past.
     */
    public boolean isOverdue(Ticket ticket) {
        if (ticket.getDueAt() == null) {
            return false;
        }
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            return false;
        }
        return ticket.getDueAt().isBefore(LocalDateTime.now());
    }

    private static final DateTimeFormatter CSV_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateCsv(List<Ticket> tickets) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,status,assignee,due,created,updated,last_touch_by\n");

        for (Ticket t : tickets) {
            String assignee = t.getAssignedTo() != null
                    ? t.getAssignedTo().getFirstName() + " " + t.getAssignedTo().getLastName()
                    : "";
            String lastTouchBy = t.getLastUpdatedBy() != null ? formatUserDisplayName(t.getLastUpdatedBy()) : "";
            sb.append(sanitizeCsv(String.valueOf(t.getId()))).append(',')
              .append(sanitizeCsv(t.getStatus() != null ? t.getStatus().name() : "")).append(',')
              .append(sanitizeCsv(assignee)).append(',')
              .append(sanitizeCsv(t.getDueAt() != null ? t.getDueAt().format(CSV_DATE_FMT) : "")).append(',')
              .append(sanitizeCsv(t.getCreatedAt() != null ? t.getCreatedAt().format(CSV_DATE_FMT) : "")).append(',')
              .append(sanitizeCsv(t.getUpdatedAt() != null ? t.getUpdatedAt().format(CSV_DATE_FMT) : "")).append(',')
              .append(sanitizeCsv(lastTouchBy))
              .append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Sanitize a CSV cell value to prevent CSV injection.
     * Prefixes dangerous formula characters (=, +, -, @, tab, CR) with a single quote
     * and escapes any double quotes by doubling them.
     */
    private String sanitizeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String sanitized = value.replace("\"", "\"\"");
        if (sanitized.charAt(0) == '=' || sanitized.charAt(0) == '+'
                || sanitized.charAt(0) == '-' || sanitized.charAt(0) == '@'
                || sanitized.charAt(0) == '\t' || sanitized.charAt(0) == '\r') {
            sanitized = "'" + sanitized;
        }
        return '"' + sanitized + '"';
    }
}
