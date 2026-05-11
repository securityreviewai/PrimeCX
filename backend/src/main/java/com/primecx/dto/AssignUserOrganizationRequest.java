package com.primecx.dto;

import jakarta.validation.constraints.NotNull;

public record AssignUserOrganizationRequest(@NotNull Long organizationId) {}
