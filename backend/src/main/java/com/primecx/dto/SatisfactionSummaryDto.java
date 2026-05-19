package com.primecx.dto;

/**
 * Roll-up of post-resolution customer ratings (all tickets in the system).
 */
public record SatisfactionSummaryDto(
        long ticketsWithRating,
        Double averageRating,
        long rating1Count,
        long rating2Count,
        long rating3Count,
        long rating4Count,
        long rating5Count,
        long ticketsWithWrittenFeedback
) {
}
