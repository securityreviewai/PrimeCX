package com.primecx.dto;

import java.time.LocalDateTime;

import com.primecx.model.AuditEventType;

public record AuditLogEntryDto(
        Long id,
        LocalDateTime createdAt,
        Long actorUserId,
        String actorEmail,
        AuditEventType eventType,
        Long targetUserId,
        Long sessionId,
        Long recordingId,
        Long ticketId,
        String detailsJson
) {}
