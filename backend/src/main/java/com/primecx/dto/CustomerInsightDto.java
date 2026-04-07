package com.primecx.dto;

import java.time.LocalDateTime;

public record CustomerInsightDto(
        Long id,
        Long userId,
        String userName,
        String insightType,
        String title,
        String description,
        String data,
        Double confidenceScore,
        LocalDateTime validUntil,
        LocalDateTime createdAt
) {}
