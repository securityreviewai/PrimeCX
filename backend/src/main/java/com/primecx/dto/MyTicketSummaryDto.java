package com.primecx.dto;

/** Compact counts for tickets visible to the current user (navbar / dashboard chips). */
public record MyTicketSummaryDto(
        long total,
        long open,
        long inProgress,
        long resolved,
        long closed
) {
}
