package com.primecx.dto;

import java.time.LocalDateTime;

public record TicketCommentDto(
        Long id,
        Long ticketId,
        Long authorUserId,
        String authorDisplayName,
        String authorRole,
        String body,
        boolean internal,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
