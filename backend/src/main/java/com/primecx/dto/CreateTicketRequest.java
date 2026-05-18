package com.primecx.dto;

import com.primecx.model.TicketCategory;
import com.primecx.model.TicketPriority;
import jakarta.validation.constraints.NotBlank;

public record CreateTicketRequest(
        @NotBlank String title,
        String description,
        TicketPriority priority,
        TicketCategory category
) {}
