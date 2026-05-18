package com.primecx.dto;

import java.time.LocalDateTime;

public record TicketMessageDto(
        Long id,
        Long ticketId,
        String body,
        LocalDateTime createdAt,
        Long authorUserId,
        String authorName
) {
}
