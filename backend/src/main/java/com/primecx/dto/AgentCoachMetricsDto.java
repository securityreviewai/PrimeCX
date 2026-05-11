package com.primecx.dto;

/**
 * Aggregate metrics for the coaching window (assigned-ticket scope only).
 */
public record AgentCoachMetricsDto(
        Double avgPickupHours,
        Double avgInProgressHours,
        int ticketsInWindow,
        int closedTicketsAnalyzed,
        int activeTicketsAwaitingReply
) {
}
