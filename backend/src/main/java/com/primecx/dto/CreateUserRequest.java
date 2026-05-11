package com.primecx.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 1, max = 100) String firstName,
        @NotBlank @Size(min = 1, max = 100) String lastName
) {}
