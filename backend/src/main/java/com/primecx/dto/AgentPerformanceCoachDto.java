package com.primecx.dto;

import java.util.List;

public record AgentPerformanceCoachDto(
        List<AgentCoachTipDto> tips,
        AgentCoachMetricsDto metrics
) {
}
