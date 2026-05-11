package com.primecx.dto;

/**
 * Aggregate resolution-quality metrics for closed tickets.
 */
public record ResolutionQualityOverviewDto(
        Double averageScore,
        long scoredClosureCount,
        long closedTicketCount,
        Double averageReopenCountAmongScored
) {}
