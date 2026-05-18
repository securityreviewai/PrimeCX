package com.primecx.service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CreateTicketRequest;
import com.primecx.dto.PagedTicketsResponse;
import com.primecx.dto.TicketDto;
import com.primecx.dto.TicketStatsResponse;
import com.primecx.dto.UpdateTicketRequest;
import com.primecx.exception.ForbiddenException;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Role;
import com.primecx.model.Ticket;
import com.primecx.model.TicketPriority;
import com.primecx.model.TicketStatus;
import com.primecx.model.User;
import com.primecx.repository.TicketRepository;
import com.primecx.repository.UserRepository;
import com.primecx.specification.TicketSpecifications;

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
        ticket.setPriority(request.priority());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setUser(user);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        log.info("Creating ticket '{}' for user {}", request.title(), userId);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket updateTicket(Long ticketId, UpdateTicketRequest request, User updater) {
        Ticket ticket = getTicketById(ticketId);
        assertCanUpdateTicket(updater, ticket);

        boolean fullAccess = updater.getRole() == Role.ROLE_SUPPORT_ADMIN
                || updater.getRole() == Role.ROLE_SUPPORT_MANAGER
                || updater.getRole() == Role.ROLE_SUPPORT_EXECUTIVE;

        if (fullAccess) {
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
        } else {
            if (request.title() != null) {
                ticket.setTitle(request.title());
            }
            if (request.description() != null) {
                ticket.setDescription(request.description());
            }
            if (request.priority() != null) {
                ticket.setPriority(request.priority());
            }
        }

        ticket.setUpdatedAt(LocalDateTime.now());
        return ticketRepository.save(ticket);
    }

    /**
     * Returns the ticket if it exists and the viewer is permitted to access it under the same rules as listing tickets.
     */
    @Transactional(readOnly = true)
    public Ticket getTicketVisibleTo(Long ticketId, User viewer) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        if (!canUserViewTicket(viewer, ticket)) {
            throw new ForbiddenException("You do not have access to this ticket.");
        }
        return ticket;
    }

    public boolean canUserViewTicket(User viewer, Ticket ticket) {
        return switch (viewer.getRole()) {
            case ROLE_SUPPORT_ADMIN, ROLE_SUPPORT_MANAGER -> true;
            case ROLE_SUPPORT_EXECUTIVE -> ticket.getAssignedTo() != null
                    && viewer.getId().equals(ticket.getAssignedTo().getId());
            case ROLE_USER -> viewer.getId().equals(ticket.getUser().getId());
        };
    }

    private void assertCanUpdateTicket(User updater, Ticket ticket) {
        switch (updater.getRole()) {
            case ROLE_SUPPORT_ADMIN, ROLE_SUPPORT_MANAGER -> {}
            case ROLE_SUPPORT_EXECUTIVE -> {
                if (ticket.getAssignedTo() == null
                        || !updater.getId().equals(ticket.getAssignedTo().getId())) {
                    throw new ForbiddenException("Only the assigned support executive can update this ticket.");
                }
            }
            case ROLE_USER -> {
                if (!updater.getId().equals(ticket.getUser().getId())) {
                    throw new ForbiddenException("You can only edit your own tickets.");
                }
            }
        }
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
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

    @Transactional(readOnly = true)
    public PagedTicketsResponse searchTickets(User viewer, TicketStatus status,
            TicketPriority priority, String q, Pageable pageable) {
        Specification<Ticket> spec = Specification.where(TicketSpecifications.visibleToUser(viewer));
        if (status != null) {
            spec = spec.and(TicketSpecifications.hasStatus(status));
        }
        if (priority != null) {
            spec = spec.and(TicketSpecifications.hasPriority(priority));
        }
        spec = spec.and(TicketSpecifications.matchesSearch(q));

        Page<Ticket> page = ticketRepository.findAll(spec, pageable);
        List<TicketDto> content = page.getContent().stream().map(this::toDto).toList();
        return new PagedTicketsResponse(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    @Transactional(readOnly = true)
    public TicketStatsResponse getTicketStats(User viewer) {
        Specification<Ticket> base = Specification.where(TicketSpecifications.visibleToUser(viewer));
        long total = ticketRepository.count(base);
        EnumMap<TicketStatus, Long> byStatus = new EnumMap<>(TicketStatus.class);
        for (TicketStatus s : TicketStatus.values()) {
            byStatus.put(s, ticketRepository.count(base.and(TicketSpecifications.hasStatus(s))));
        }
        long activeCount = byStatus.getOrDefault(TicketStatus.OPEN, 0L)
                + byStatus.getOrDefault(TicketStatus.IN_PROGRESS, 0L);
        long resolvedCount = byStatus.getOrDefault(TicketStatus.RESOLVED, 0L)
                + byStatus.getOrDefault(TicketStatus.CLOSED, 0L);
        return new TicketStatsResponse(byStatus, total, activeCount, resolvedCount);
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
                ticket.getUpdatedAt()
        );
    }
}
