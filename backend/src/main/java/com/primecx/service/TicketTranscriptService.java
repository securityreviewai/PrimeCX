package com.primecx.service;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.TicketDto;
import com.primecx.dto.TicketTimelineEntryDto;
import com.primecx.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketTranscriptService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TicketService ticketService;
    private final TicketTimelineService ticketTimelineService;

    @Transactional(readOnly = true)
    public String buildPlainTextTranscript(Long ticketId, User viewer) {
        var ticket = ticketService.getTicketVisibleTo(ticketId, viewer);
        TicketDto dto = ticketService.toDto(ticket);
        StringBuilder sb = new StringBuilder();
        sb.append("Ticket #").append(dto.id())
                .append(" — ").append(dto.title())
                .append("\nStatus: ").append(dto.status())
                .append("  Priority: ").append(dto.priority())
                .append("  Category: ").append(dto.category())
                .append("\nRequester: ").append(dto.userName()).append(" (user #").append(dto.userId()).append(')');
        if (dto.assignedToName() != null) {
            sb.append("\nAssignee: ").append(dto.assignedToName())
                    .append(" (user #").append(dto.assignedToId()).append(')');
        }
        sb.append("\n\n--- Timeline ---\n");
        for (TicketTimelineEntryDto e : ticketTimelineService.buildTimeline(ticketId, viewer)) {
            sb.append('[').append(e.at() != null ? TS.format(e.at()) : "?").append("] ");
            if ("message".equalsIgnoreCase(e.kind())) {
                boolean internal = Boolean.TRUE.equals(e.messageInternalNote());
                sb.append("MESSAGE");
                if (internal) {
                    sb.append(" [internal]");
                }
                sb.append(" — ").append(nvl(e.messageAuthorName())).append(": ")
                        .append(normalizeNewlines(nvl(e.messageBody())));
            } else {
                sb.append("ACTIVITY — ")
                        .append(e.activityType() != null ? e.activityType() : "?")
                        .append(" — ").append(nvl(e.activityActorName()))
                        .append(" — ").append(nvl(e.activitySummary()));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String normalizeNewlines(String body) {
        return body.replace("\r\n", "\n").replace('\r', '\n').trim();
    }
}
