package com.primecx.dto;

import java.util.List;

public record MultipartCompleteRequest(
        Long sessionId,
        String s3Key,
        String uploadId,
        String fileName,
        String contentType,
        Long fileSize,
        Integer duration,
        List<CompletedPartDto> parts
) {}
