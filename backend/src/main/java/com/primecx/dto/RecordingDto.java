package com.primecx.dto;

import java.time.LocalDateTime;

public record RecordingDto(
        Long id,
        Long sessionId,
        String s3Key,
        String fileName,
        Long fileSize,
        Integer durationSeconds,
        String contentType,
        LocalDateTime uploadedAt,
        String presignedUrl
) {}
