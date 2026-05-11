package com.primecx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTicketCommentRequest(
        @NotBlank
        @Size(max = 16_000)
        String body,
        /**
         * Only support roles may set; null means leave unchanged.
         */
        Boolean internal
) {}
