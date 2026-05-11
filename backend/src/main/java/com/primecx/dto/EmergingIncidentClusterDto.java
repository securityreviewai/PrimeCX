package com.primecx.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.primecx.model.TicketCategory;

/**
 * A group of recent tickets that likely describe the same emerging issue (outage / widespread problem).
 */
public record EmergingIncidentClusterDto(
        String clusterId,
        int ticketCount,
        List<Long> ticketIds,
        List<String> sampleTitles,
        TicketCategory dominantCategory,
        LocalDateTime firstReportedAt,
        LocalDateTime lastReportedAt,
        String riskLevel,
        double cohesionScore
) {
}
