package com.primecx.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for sentiment timeline query.
 * Contains paginated sentiment entries and escalation indicators.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentTimelineResponse {

    private Long customerId;

    private List<SentimentTimelineEntryDto> entries;

    private Integer totalCount;

    private Integer currentPage;

    private Integer pageSize;

    /** Indicates if there are escalation risks in the timeline */
    private Boolean hasEscalationRisks;

    /** Overall sentiment trend: IMPROVING, STABLE, DECLINING */
    private String sentimentTrend;

    /** Average sentiment score across timeline */
    private Double averageSentimentScore;
}
