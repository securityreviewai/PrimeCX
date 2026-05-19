package com.primecx.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.TicketActivityDto;
import com.primecx.dto.TicketMessageDto;
import com.primecx.dto.TicketTimelineEntryDto;
import com.primecx.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketTimelineService {

    private final TicketService ticketService;
    private final TicketActivityService ticketActivityService;
    private final TicketMessageService ticketMessageService;

    @Transactional(readOnly = true)
    public List<TicketTimelineEntryDto> buildTimeline(Long ticketId, User viewer) {
        ticketService.getTicketVisibleTo(ticketId, viewer);
        List<TicketTimelineEntryDto> merged = new ArrayList<>();

        for (TicketActivityDto a : ticketActivityService.listAllForTicketAscending(ticketId)) {
            merged.add(new TicketTimelineEntryDto(
                    "activity",
                    a.createdAt(),
                    a.id(),
                    a.eventType(),
                    a.summary(),
                    a.actorUserId(),
                    a.actorName(),
                    null,
                    null,
                    null,
                    null,
                    null));
        }
        for (TicketMessageDto m : ticketMessageService.listMessages(ticketId, viewer)) {
            merged.add(new TicketTimelineEntryDto(
                    "message",
                    m.createdAt(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    m.id(),
                    m.body(),
                    m.authorUserId(),
                    m.authorName(),
                    m.internalNote()));
        }

        merged.sort(Comparator.comparing(TicketTimelineEntryDto::at, Comparator.nullsLast(Comparator.naturalOrder())));
        return merged;
    }
}
