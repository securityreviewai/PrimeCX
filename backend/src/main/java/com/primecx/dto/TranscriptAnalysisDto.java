package com.primecx.dto;

import com.primecx.model.AnalysisStatus;
import com.primecx.model.SentimentType;

import java.time.LocalDateTime;
import java.util.List;

public record TranscriptAnalysisDto(
        Long id,
        Long sessionId,
        Long ticketId,
        String ticketTitle,
        String executiveName,
        SentimentType sentimentType,
        Double sentimentScore,
        String summary,
        List<String> keyTopics,
        Double customerSatisfactionScore,
        String resolutionQuality,
        List<String> suggestedFollowUps,
        String escalationRisk,
        AnalysisStatus status,
        LocalDateTime analyzedAt
) {}
