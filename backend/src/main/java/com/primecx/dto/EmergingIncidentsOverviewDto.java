package com.primecx.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of scanning recent active tickets for similarity clusters (proactive incident detection).
 */
public record EmergingIncidentsOverviewDto(
        List<EmergingIncidentClusterDto> clusters,
        int scannedTicketCount,
        int windowHours,
        int minClusterSize,
        LocalDateTime windowStart,
        LocalDateTime computedAt,
        String detectionMethod
) {
}
