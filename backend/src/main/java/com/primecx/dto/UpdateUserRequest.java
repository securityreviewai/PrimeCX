package com.primecx.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 1, max = 100) String firstName,
        @Size(min = 1, max = 100) String lastName,
        @Size(max = 255) String email
) {}
