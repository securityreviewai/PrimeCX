package com.primecx.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.PagedTicketActivityResponse;
import com.primecx.dto.TicketActivityDto;
import com.primecx.model.Ticket;
import com.primecx.model.TicketActivityLog;
import com.primecx.model.TicketActivityType;
import com.primecx.model.User;
import com.primecx.repository.TicketActivityLogRepository;
import com.primecx.repository.TicketRepository;
import com.primecx.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketActivityService {

    private final TicketActivityLogRepository activityLogRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Transactional
    public void record(Long ticketId, User actor, TicketActivityType type, String summary) {
        Ticket ticketRef = ticketRepository.getReferenceById(ticketId);
        TicketActivityLog row = new TicketActivityLog();
        row.setTicket(ticketRef);
        if (actor != null) {
            row.setActor(userRepository.getReferenceById(actor.getId()));
        }
        row.setEventType(type);
        row.setSummary(truncate(summary, 1024));
        activityLogRepository.save(row);
        log.debug("Recorded {} on ticket {}", type, ticketId);
    }

    @Transactional(readOnly = true)
    public PagedTicketActivityResponse listForTicket(Long ticketId, Pageable pageable) {
        Page<TicketActivityLog> page = activityLogRepository.findByTicket_IdOrderByCreatedAtDesc(ticketId, pageable);
        var content = page.getContent().stream().map(this::toDto).toList();
        return new PagedTicketActivityResponse(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    private TicketActivityDto toDto(TicketActivityLog row) {
        User actor = row.getActor();
        Long actorId = actor != null ? actor.getId() : null;
        String actorName = actor != null
                ? (actor.getFirstName() + " " + actor.getLastName()).strip()
                : "System";
        return new TicketActivityDto(
                row.getId(),
                row.getEventType(),
                row.getSummary(),
                row.getCreatedAt(),
                actorId,
                actorName);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }
}
