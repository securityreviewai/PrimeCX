package com.primecx.dto;

import java.time.LocalDateTime;
import com.primecx.model.SentimentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single sentiment data point for a customer interaction.
 * Part of the sentiment timeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentTimelineEntryDto {

    private Long sessionId;

    private Long ticketId;

    @NotNull(message = "Sentiment type is required")
    private SentimentType sentimentType;

    @Min(value = 0, message = "Sentiment score must be between 0 and 1")
    @Max(value = 1, message = "Sentiment score must be between 0 and 1")
    private Double sentimentScore;

    private Double customerSatisfactionScore;

    private String escalationRisk;

    private String summary;

    private LocalDateTime sessionStartTime;

    private LocalDateTime sessionEndTime;

    private LocalDateTime analyzedAt;
}
