package com.primecx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSavedReplyRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 4096) String body
) {}
