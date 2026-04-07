package com.primecx.dto;

import com.primecx.model.SessionStatus;

import java.time.LocalDateTime;

public record SupportSessionDto(
        Long id,
        Long ticketId,
        String ticketTitle,
        Long supportExecutiveId,
        String supportExecutiveName,
        Long userId,
        String userName,
        SessionStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String notes
) {}
