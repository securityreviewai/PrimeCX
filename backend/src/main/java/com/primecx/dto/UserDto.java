package com.primecx.dto;

import com.primecx.model.Role;

public record UserDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        Role role,
        boolean active
) {}
