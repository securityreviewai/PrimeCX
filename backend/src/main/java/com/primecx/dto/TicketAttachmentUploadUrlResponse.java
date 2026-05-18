package com.primecx.dto;

public record TicketAttachmentUploadUrlResponse(String uploadUrl, String s3Key, String contentTypeForUpload) {
}
