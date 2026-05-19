package com.primecx.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MultipartInitRequest(
        Long sessionId,
        String fileName,
        String contentType
) {}
