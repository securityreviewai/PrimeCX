package com.primecx.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ConfirmTicketAttachmentRequest(
        @NotBlank String s3Key,
        @NotBlank @Size(max = 512) String fileName,
        @NotBlank @Size(max = 255) String contentType,
        @NotNull @Positive @Max(26_214_400) Long fileSize
) {
}
