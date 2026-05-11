package com.primecx.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCannedResponseRequest(
        @Size(max = 80) @Pattern(regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$", message = "must be lowercase alphanumeric with hyphens, 2-80 chars")
        String shortcode,
        @Size(max = 200)
        String title,
        @Size(max = 10000)
        String content,
        @Size(max = 80)
        String category
) {}
