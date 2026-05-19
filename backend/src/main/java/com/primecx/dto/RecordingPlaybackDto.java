package com.primecx.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RecordingPlaybackDto(
        Long id,
        Long sessionId,
        String fileName,
        Long fileSize,
        Integer durationSeconds,
        String contentType,
        LocalDateTime uploadedAt,
        String presignedUrl,
        String transcript,
        List<RedactionRegionDto> redactionRegions,
        LocalDateTime retentionExpiresAt,
        List<RecordingQaReviewDto> qaReviews
) {}
