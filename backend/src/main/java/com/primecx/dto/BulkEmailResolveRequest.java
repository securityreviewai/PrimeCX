package com.primecx.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BulkEmailResolveRequest(
        @NotEmpty
        @Size(max = 50)
        List<@NotBlank @Email String> emails
) {
}
