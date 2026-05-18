package com.primecx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketAttachmentUploadRequest(
        @NotBlank @Size(max = 512) String fileName,
        @NotBlank @Size(max = 255) String contentType
) {
}
