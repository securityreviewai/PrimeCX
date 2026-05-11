package com.primecx.dto;

import com.primecx.model.TicketPriority;
import com.primecx.model.TicketStatus;

public record UpdateTicketRequest(
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        Long assignedToId,
        String internalNotes
) {}
