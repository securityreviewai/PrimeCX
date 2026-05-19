package com.primecx.dto;

import java.time.LocalDateTime;

public record TicketMessageDto(
        Long id,
        Long ticketId,
        Long authorId,
        String authorName,
        String body,
        boolean internalNote,
        LocalDateTime createdAt
) {}
