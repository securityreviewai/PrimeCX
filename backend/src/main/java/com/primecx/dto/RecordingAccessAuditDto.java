package com.primecx.dto;

import java.time.LocalDateTime;

public record RecordingAccessAuditDto(
        Long id,
        Long recordingId,
        Long userId,
        String userName,
        String accessType,
        String ipAddress,
        LocalDateTime accessedAt
) {}
