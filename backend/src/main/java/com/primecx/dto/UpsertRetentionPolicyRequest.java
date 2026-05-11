package com.primecx.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertRetentionPolicyRequest(
        @NotBlank @Size(max = 255) String s3Bucket,
        /** When null, upserts the default policy for this bucket (all orgs unless an org-specific row exists). */
        Long organizationId,
        @NotNull Integer retentionDays,
        @NotNull Integer softDeleteGraceDays
) {}
