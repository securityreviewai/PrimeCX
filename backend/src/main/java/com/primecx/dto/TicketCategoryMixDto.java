package com.primecx.dto;

import com.primecx.model.TicketCategory;

/** Tickets created in the reporting window, grouped by category. */
public record TicketCategoryMixDto(
        TicketCategory category,
        long createdCount
) {
}
