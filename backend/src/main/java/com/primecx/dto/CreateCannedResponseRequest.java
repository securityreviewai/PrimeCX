package com.primecx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCannedResponseRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$", message = "must be lowercase alphanumeric with hyphens, 2-80 chars")
        String shortcode,
        @NotBlank @Size(max = 200)
        String title,
        @NotBlank @Size(max = 10000)
        String content,
        @Size(max = 80)
        String category
) {}
