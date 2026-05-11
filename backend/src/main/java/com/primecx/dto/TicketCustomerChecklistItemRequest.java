package com.primecx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TicketCustomerChecklistItemRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[a-zA-Z0-9_-]+") String id,
        @NotBlank @Size(max = 500) String label,
        boolean completed
) {}
