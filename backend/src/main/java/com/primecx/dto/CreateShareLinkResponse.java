package com.primecx.dto;

import java.time.LocalDateTime;

public record CreateShareLinkResponse(
        Long id,
        Long sessionId,
        String token,
        String shortPath,
        LocalDateTime expiresAt
) {}
