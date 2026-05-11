package com.primecx.dto;

import com.primecx.model.Role;

import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull Role role
) {}
