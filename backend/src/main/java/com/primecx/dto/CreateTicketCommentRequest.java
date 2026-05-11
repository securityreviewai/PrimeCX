package com.primecx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketCommentRequest(
        @NotBlank
        @Size(max = 16_000)
        String body,
        /**
         * When true, only support roles may create; end users' requests are treated as false.
         */
        Boolean internal
) {}
