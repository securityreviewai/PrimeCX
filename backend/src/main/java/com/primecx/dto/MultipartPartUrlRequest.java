package com.primecx.dto;

public record MultipartPartUrlRequest(
        String s3Key,
        String uploadId,
        Integer partNumber
) {}
