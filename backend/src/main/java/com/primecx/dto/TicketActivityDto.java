package com.primecx.dto;

import java.time.LocalDateTime;

import com.primecx.model.TicketActivityType;

public record TicketActivityDto(
        Long id,
        TicketActivityType eventType,
        String summary,
        LocalDateTime createdAt,
        Long actorUserId,
        String actorName
) {
}
