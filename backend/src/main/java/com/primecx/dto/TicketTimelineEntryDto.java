package com.primecx.dto;

import java.time.LocalDateTime;

import com.primecx.model.TicketActivityType;

/**
 * Unified chronological item for ticket detail (activity log + customer & staff messages).
 */
public record TicketTimelineEntryDto(
        String kind,
        LocalDateTime at,
        Long activityId,
        TicketActivityType activityType,
        String activitySummary,
        Long activityActorUserId,
        String activityActorName,
        Long messageId,
        String messageBody,
        Long messageAuthorUserId,
        String messageAuthorName,
        Boolean messageInternalNote
) {
}
