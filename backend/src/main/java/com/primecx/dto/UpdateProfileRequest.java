package com.primecx.dto;

import com.primecx.model.Role;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
}
