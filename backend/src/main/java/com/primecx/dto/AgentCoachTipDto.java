package com.primecx.dto;

import java.util.List;

/**
 * A single coaching suggestion derived from ticket history (stage timing, follow-ups, tone heuristics).
 */
public record AgentCoachTipDto(
        String category,
        String severity,
        String title,
        String detail,
        List<Long> exampleTicketIds
) {
}
