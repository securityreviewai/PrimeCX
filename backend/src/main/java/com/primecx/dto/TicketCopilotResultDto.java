package com.primecx.dto;

import com.primecx.model.TicketPriority;

import java.util.List;

/**
 * Validated AI ticket copilot output for the API.
 */
public record TicketCopilotResultDto(
        String threadSummary,
        List<String> suggestedReplies,
        TicketPriority suggestedUrgency,
        String urgencyReasoning,
        String nextBestActionTitle,
        String nextBestActionDetail,
        double confidence
) {}
