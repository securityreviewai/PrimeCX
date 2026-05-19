package com.primecx.dto;

public record MultipartPartUrlResponse(
        String uploadUrl,
        Integer partNumber
) {}
