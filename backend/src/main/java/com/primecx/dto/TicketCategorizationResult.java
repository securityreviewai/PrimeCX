package com.primecx.dto;

import java.util.List;

public record TicketCategorizationResult(
        Long ticketId,
        String suggestedCategory,
        String suggestedPriority,
        String reasoning,
        List<Long> similarTicketIds
) {}
