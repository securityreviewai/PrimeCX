package com.primecx.dto;

public record SlaComplianceSummaryDto(
        long totalTickets,
        long responseMetCount,
        long responseBreachedCount,
        long responsePendingCount,
        long resolveMetCount,
        long resolveBreachedCount,
        long resolvePendingCount,
        double responseCompliancePercent,
        double resolveCompliancePercent
) {}
