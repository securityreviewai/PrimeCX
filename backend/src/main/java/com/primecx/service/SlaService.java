package com.primecx.service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.model.Ticket;
import com.primecx.model.TicketPriority;
import com.primecx.model.TicketStatus;
import com.primecx.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaService {

    private final TicketRepository ticketRepository;

    private static final Map<TicketPriority, SlaTargets> TARGETS = new EnumMap<>(TicketPriority.class);

    static {
        TARGETS.put(TicketPriority.CRITICAL, new SlaTargets(1, 4));
        TARGETS.put(TicketPriority.HIGH, new SlaTargets(4, 24));
        TARGETS.put(TicketPriority.MEDIUM, new SlaTargets(8, 48));
        TARGETS.put(TicketPriority.LOW, new SlaTargets(24, 72));
    }

    public void applySlaDeadlines(Ticket ticket) {
        SlaTargets targets = TARGETS.getOrDefault(ticket.getPriority(), TARGETS.get(TicketPriority.MEDIUM));
        LocalDateTime created = ticket.getCreatedAt() != null ? ticket.getCreatedAt() : LocalDateTime.now();
        ticket.setSlaRespondBy(created.plusHours(targets.respondHours()));
        ticket.setSlaResolveBy(created.plusHours(targets.resolveHours()));
    }

    @Transactional
    public void recordFirstResponse(Ticket ticket) {
        if (ticket.getFirstRespondedAt() != null) {
            return;
        }
        ticket.setFirstRespondedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        log.info("First response recorded for ticket {}", ticket.getId());
    }

    public boolean isResponseBreached(Ticket ticket) {
        if (ticket.getSlaRespondBy() == null) {
            return false;
        }
        return ticket.getFirstRespondedAt() == null
                && LocalDateTime.now().isAfter(ticket.getSlaRespondBy());
    }

    public boolean isResponseAtRisk(Ticket ticket) {
        if (ticket.getSlaRespondBy() == null || ticket.getFirstRespondedAt() != null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isAfter(ticket.getSlaRespondBy())
                && now.isAfter(ticket.getSlaRespondBy().minusHours(2));
    }

    public boolean isResolveBreached(Ticket ticket) {
        if (ticket.getSlaResolveBy() == null) {
            return false;
        }
        return !isResolved(ticket) && LocalDateTime.now().isAfter(ticket.getSlaResolveBy());
    }

    public boolean isResolveAtRisk(Ticket ticket) {
        if (ticket.getSlaResolveBy() == null || isResolved(ticket)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isAfter(ticket.getSlaResolveBy())
                && now.isAfter(ticket.getSlaResolveBy().minusHours(2));
    }

    public boolean isResponseMet(Ticket ticket) {
        if (ticket.getFirstRespondedAt() == null || ticket.getSlaRespondBy() == null) {
            return false;
        }
        return !ticket.getFirstRespondedAt().isAfter(ticket.getSlaRespondBy());
    }

    public boolean isResolveMet(Ticket ticket) {
        if (!isResolved(ticket) || ticket.getSlaResolveBy() == null) {
            return false;
        }
        LocalDateTime resolvedAt = ticket.getUpdatedAt() != null ? ticket.getUpdatedAt() : LocalDateTime.now();
        return !resolvedAt.isAfter(ticket.getSlaResolveBy());
    }

    private boolean isResolved(Ticket ticket) {
        return ticket.getStatus() == TicketStatus.RESOLVED
                || ticket.getStatus() == TicketStatus.CLOSED;
    }

    private record SlaTargets(int respondHours, int resolveHours) {}
}
