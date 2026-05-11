package com.primecx.dto;

import jakarta.validation.constraints.NotNull;

public record PatchCustomerChecklistItemRequest(
        @NotNull Boolean completed
) {}
