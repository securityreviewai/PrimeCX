package com.primecx.dto;

import java.util.List;

import com.primecx.model.TicketCategory;
import com.primecx.model.TicketIntent;

public record TicketTriageDto(
        TicketIntent intent,
        TicketCategory category,
        List<String> tags,
        String workflowKey,
        String workflowLabel,
        String templateKey,
        String templateTitle,
        String templateBody,
        Double confidence,
        String rationale
) {}
