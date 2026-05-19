package com.primecx.dto;

public record MultipartInitResponse(
        String uploadId,
        String s3Key,
        Long recordingUploadId
) {}
