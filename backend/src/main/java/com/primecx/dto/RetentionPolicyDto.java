package com.primecx.dto;

public record RetentionPolicyDto(
        int retentionDays,
        int transitionToIaDays,
        int transitionToGlacierDays,
        boolean autoTranscriptionEnabled,
        boolean autoAnalysisOnSessionEnd
) {}
