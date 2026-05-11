package com.primecx.dto;

import com.primecx.model.TicketPriority;
import com.primecx.model.TicketStatus;

import java.time.LocalDateTime;

public record TicketDto(
        Long id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        Long userId,
        String userName,
        Long assignedToId,
        String assignedToName,
        String internalNotes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
