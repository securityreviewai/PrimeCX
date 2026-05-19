package com.primecx.dto;

import java.time.LocalDate;

/** One bucket for charting ticket creation volume over time. */
public record TicketVolumeBucketDto(
        LocalDate day,
        long createdCount
) {
}
