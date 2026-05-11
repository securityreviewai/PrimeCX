package com.primecx.dto;

import java.time.LocalDateTime;

public record CannedResponseDto(
        Long id,
        String shortcode,
        String title,
        String content,
        String category,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
