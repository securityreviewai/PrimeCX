package com.primecx.dto;

import java.time.LocalDateTime;

/**
 * Rough throughput metric: for tickets marked RESOLVED/CLOSED whose {@code updated_at} falls in the window,
 * average hours between creation and last update (proxy for time active in queue).
 */
public record ResolutionTimeSummaryDto(
        LocalDateTime windowStartInclusive,
        LocalDateTime windowEndExclusive,
        long closedTicketsInPeriod,
        Double averageHoursCreationToLastUpdate
) {
}
