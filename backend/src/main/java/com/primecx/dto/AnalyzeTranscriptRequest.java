package com.primecx.dto;

public record AnalyzeTranscriptRequest(
        Long sessionId,
        String transcript
) {}
