package com.primecx.dto;

import java.time.LocalDateTime;

public record RecordingQaReviewDto(
        Long id,
        Long recordingId,
        Long reviewerId,
        String reviewerName,
        Integer empathyScore,
        Integer accuracyScore,
        Integer complianceScore,
        Integer overallScore,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
