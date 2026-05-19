package com.primecx.dto;

import java.util.List;

public record CompletedPartDto(
        Integer partNumber,
        String etag
) {}
