package com.primecx.dto;

import java.util.Map;

import com.primecx.model.TicketStatus;

public record TicketStatsResponse(
        Map<TicketStatus, Long> byStatus,
        long total,
        long activeCount,
        long resolvedCount
) {
}
