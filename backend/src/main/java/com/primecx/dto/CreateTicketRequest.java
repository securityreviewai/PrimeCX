package com.primecx.dto;

import java.time.LocalDateTime;

import com.primecx.model.TicketPriority;
import jakarta.validation.constraints.NotBlank;

public record CreateTicketRequest(
        @NotBlank String title,
        String description,
        TicketPriority priority,
        LocalDateTime dueAt
) {}
