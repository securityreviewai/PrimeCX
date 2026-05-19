package com.primecx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketMessageRequest(
        @NotBlank @Size(max = 10000) String body,
        boolean internalNote
) {}
