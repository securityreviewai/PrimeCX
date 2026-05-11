package com.primecx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CallToTicketRequest(
        @NotNull @Positive Long customerUserId,
        @NotBlank @Size(max = 50_000) String transcript
) {}
