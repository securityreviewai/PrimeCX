package com.primecx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketMessageRequest(
        @NotBlank @Size(max = 4096) String body,
        Boolean internalNote
) {
}
