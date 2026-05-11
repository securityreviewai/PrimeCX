package com.primecx.dto;

public record RetentionPolicyDto(
        Long id,
        Long organizationId,
        String organizationName,
        String s3Bucket,
        int retentionDays,
        int softDeleteGraceDays
) {}
