package com.primecx.dto;

import java.time.LocalDateTime;

public record TicketAttachmentDto(
        Long id,
        Long ticketId,
        String fileName,
        String contentType,
        Long fileSize,
        String uploadedByName,
        LocalDateTime uploadedAt,
        String downloadUrl
) {
}
