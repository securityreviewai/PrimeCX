package com.primecx.dto;

import java.util.List;
import java.util.Map;

public record AIInsightsSummary(
        Double overallSentimentScore,
        Long totalAnalyses,
        Double averageSatisfactionScore,
        List<String> topIssueCategories,
        Map<String, Long> escalationRiskBreakdown,
        List<CustomerInsightDto> recentInsights,
        Map<String, Long> sentimentDistribution
) {}
