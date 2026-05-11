package com.primecx.dto;

import java.time.LocalDateTime;

import com.primecx.model.SessionStatus;

public record ResolvedShareLinkDto(
        Long sessionId,
        Long ticketId,
        String ticketTitle,
        SessionStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime expiresAt
) {}
