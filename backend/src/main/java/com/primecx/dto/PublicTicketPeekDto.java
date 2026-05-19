package com.primecx.dto;

import com.primecx.model.TicketStatus;

/**
 * Minimal ticket projection for the public peek endpoint (see {@link com.primecx.controller.PublicTicketPeekController}).
 */
public record PublicTicketPeekDto(
        Long id,
        String title,
        TicketStatus status,
        /** Echo of request query param {@code source} — unchecked client input. */
        String integrationSource
) {
}
