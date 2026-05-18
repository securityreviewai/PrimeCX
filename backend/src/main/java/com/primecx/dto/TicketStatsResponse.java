package com.primecx.dto;

import java.util.Map;

import com.primecx.model.TicketCategory;
import com.primecx.model.TicketStatus;

public record TicketStatsResponse(
        Map<TicketStatus, Long> byStatus,
        Map<TicketCategory, Long> byCategory,
        long total,
        long activeCount,
        long resolvedCount
) {
}
