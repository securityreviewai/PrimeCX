package com.primecx.dto;

import com.primecx.model.TicketCategory;
import com.primecx.model.TicketPriority;
import com.primecx.model.TicketStatus;

import java.time.LocalDateTime;

public record TicketDto(
        Long id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        TicketCategory category,
        Long userId,
        String userName,
        Long assignedToId,
        String assignedToName,
        String internalNotes,
        String supportReply,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean escalated
) {}
