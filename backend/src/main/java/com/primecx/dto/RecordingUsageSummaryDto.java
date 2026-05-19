package com.primecx.dto;

/** Aggregate metadata across all stored session recordings. */
public record RecordingUsageSummaryDto(
        long recordingCount,
        long totalFileBytes,
        long totalDurationSeconds
) {
}
