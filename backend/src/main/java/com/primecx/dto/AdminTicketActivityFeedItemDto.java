package com.primecx.dto;

import java.time.LocalDateTime;

import com.primecx.model.TicketActivityType;

/** One row for the cross-ticket admin activity feed (ticket context included). */
public record AdminTicketActivityFeedItemDto(
        Long activityId,
        Long ticketId,
        String ticketTitle,
        TicketActivityType eventType,
        String summary,
        LocalDateTime createdAt,
        Long actorUserId,
        String actorName
) {
}
