package com.primecx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSavedReplyRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 4096) String body
) {}
