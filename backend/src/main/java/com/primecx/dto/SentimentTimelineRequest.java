package com.primecx.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for fetching sentiment timeline for a customer.
 * Enforces server-side validation and constrains input parameters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentTimelineRequest {

    @NotNull(message = "Customer ID is required")
    @Min(value = 1, message = "Customer ID must be positive")
    private Long customerId;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Min(value = 0, message = "Page number must be >= 0")
    @Builder.Default
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be >= 1")
    @Builder.Default
    private Integer pageSize = 20;
}
