package com.primecx.dto;

import com.primecx.model.TicketPriority;
import com.primecx.model.TicketStatus;

import java.time.LocalDateTime;

public record PublicTicketPeekDto(
        Long id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        String customerName,
        String customerEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
