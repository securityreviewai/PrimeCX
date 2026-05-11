package com.primecx.dto;

import java.time.LocalDateTime;

/**
 * @param presignedUrl            optional; only for flows that pre-sign
 * @param legalHold              null when not requested for list responses
 * @param deletedAt              when soft-deleted; null for active
 * @param s3Bucket               may be null in legacy rows (avoid exposing to non-admins)
 * @param retentionPolicySummary one-line description for admin UI, optional
 */
public record RecordingDto(
        Long id,
        Long sessionId,
        String s3Key,
        String fileName,
        Long fileSize,
        Integer durationSeconds,
        String contentType,
        LocalDateTime uploadedAt,
        String presignedUrl,
        Boolean legalHold,
        LocalDateTime deletedAt,
        String s3Bucket,
        String retentionPolicySummary
) {
    public static RecordingDto basic(
            Long id,
            Long sessionId,
            String s3Key,
            String fileName,
            Long fileSize,
            Integer durationSeconds,
            String contentType,
            LocalDateTime uploadedAt,
            String presignedUrl) {
        return new RecordingDto(
                id, sessionId, s3Key, fileName, fileSize, durationSeconds, contentType, uploadedAt, presignedUrl,
                null, null, null, null);
    }
}
