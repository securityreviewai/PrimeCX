package com.primecx.dto;

import java.time.LocalDateTime;

public record TicketChangeLogDto(
        Long id,
        String fieldName,
        String oldValue,
        String newValue,
        Long changedByUserId,
        String changedByName,
        LocalDateTime changedAt
) {}
