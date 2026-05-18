package com.primecx.dto;

import com.primecx.model.TicketCategory;
import com.primecx.model.TicketPriority;
import com.primecx.model.TicketStatus;

public record UpdateTicketRequest(
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        TicketCategory category,
        Long assignedToId
) {}
