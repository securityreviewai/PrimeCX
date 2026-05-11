package com.primecx.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.SentimentTimelineEntryDto;
import com.primecx.dto.SentimentTimelineRequest;
import com.primecx.dto.SentimentTimelineResponse;
import com.primecx.model.SentimentType;
import com.primecx.model.TranscriptAnalysis;
import com.primecx.model.User;
import com.primecx.repository.SentimentTimelineRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for sentiment timeline retrieval and analysis.
 * 
 * Enforces security guardrails:
 * - 111: Least-privilege authorization with deny-by-default
 * - 134: Method-level @PreAuthorize for tenant scoping
 * - 138: Enable method security for service-layer authorization
 * - 139: Prevent BOLA with object-aware authorization logic
 * - 121: Log security-relevant events (sentiment data access)
 * - 113: Validate and canonicalize input server-side (via DTOs)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SentimentTimelineService {

    private final SentimentTimelineRepository sentimentTimelineRepository;
    private final UserService userService;

    /**
     * Get sentiment timeline for a customer.
     * 
     * Authorization: User must be viewing their own timeline OR be a support agent/admin
     * with permission to view customer data (guardrail 134, 139, 172).
     * 
     * @param request Validated sentiment timeline request with customerId, date range, pagination
     * @param currentUserId ID of the requesting user (from security context)
     * @return Paginated sentiment timeline with trend analysis
     */
    @PreAuthorize("@authz.canViewCustomerSentiment(#request.customerId, authentication)")
    public SentimentTimelineResponse getSentimentTimeline(
            SentimentTimelineRequest request,
            Long currentUserId) {

        // Input validation already performed by @Valid on request
        Long customerId = request.getCustomerId();
        LocalDateTime startDate = request.getStartDate();
        LocalDateTime endDate = request.getEndDate();

        // Set default date range if not provided (last 90 days)
        if (startDate == null || endDate == null) {
            endDate = LocalDateTime.now();
            startDate = endDate.minus(90, ChronoUnit.DAYS);
        }

        // Validate date range is reasonable (prevent resource exhaustion)
        if (startDate.isAfter(endDate)) {
            startDate = endDate.minus(90, ChronoUnit.DAYS);
        }

        int page = request.getPage() != null ? request.getPage() : 0;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        // Clamp page size to prevent DOS (guardrail 172 - implicit rate limiting)
        if (pageSize > 100) {
            pageSize = 100;
        }

        Pageable pageable = PageRequest.of(page, pageSize);

        // BOLA prevention: Query is scoped to customerId at repository layer (guardrail 136)
        Page<TranscriptAnalysis> sentimentPage = sentimentTimelineRepository
                .findCustomerSentimentTimeline(customerId, startDate, endDate, pageable);

        // Log sentiment data access for audit trail (guardrail 121)
        log.info("SECURITY_AUDIT: Sentiment timeline accessed for customer={}, by user={}, dateRange={} to {}",
                customerId, currentUserId, startDate, endDate);

        // Convert to DTOs
        List<SentimentTimelineEntryDto> entries = sentimentPage.getContent()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        // Calculate trend analysis
        String sentimentTrend = calculateTrend(entries);
        Double averageSentiment = calculateAverageSentiment(entries);

        // Check for escalation risks
        Long escalationCount = sentimentTimelineRepository.countEscalationRisks(
                customerId, startDate, endDate);
        Boolean hasEscalationRisks = escalationCount > 0;

        // Gardrail 120: Do not return verbose errors - return only necessary data
        return SentimentTimelineResponse.builder()
                .customerId(customerId)
                .entries(entries)
                .totalCount((int) sentimentPage.getTotalElements())
                .currentPage(page)
                .pageSize(pageSize)
                .hasEscalationRisks(hasEscalationRisks)
                .sentimentTrend(sentimentTrend)
                .averageSentimentScore(averageSentiment)
                .build();
    }

    /**
     * Check if customer has any escalation risks.
     * Helper method for dashboard and early warning systems.
     * 
     * @param customerId Customer ID
     * @param lookbackDays How many days to look back
     * @return true if escalation risks found, false otherwise
     */
    @PreAuthorize("@authz.canViewCustomerSentiment(#customerId, authentication)")
    public Boolean hasEscalationRisks(Long customerId, Integer lookbackDays) {
        if (lookbackDays == null) {
            lookbackDays = 30;
        }

        LocalDateTime startDate = LocalDateTime.now().minus(lookbackDays, ChronoUnit.DAYS);
        LocalDateTime endDate = LocalDateTime.now();

        Long escalationCount = sentimentTimelineRepository.countEscalationRisks(
                customerId, startDate, endDate);

        return escalationCount > 0;
    }

    /**
     * Map TranscriptAnalysis to DTO.
     * Removes sensitive internal data, only exposing necessary fields.
     * 
     * @param analysis The transcript analysis entity
     * @return DTO safe for client exposure
     */
    private SentimentTimelineEntryDto mapToDto(TranscriptAnalysis analysis) {
        return SentimentTimelineEntryDto.builder()
                .sessionId(analysis.getSession() != null ? analysis.getSession().getId() : null)
                .ticketId(analysis.getSession() != null && analysis.getSession().getTicket() != null 
                    ? analysis.getSession().getTicket().getId() : null)
                .sentimentType(analysis.getSentimentType())
                .sentimentScore(analysis.getSentimentScore())
                .customerSatisfactionScore(analysis.getCustomerSatisfactionScore())
                .escalationRisk(analysis.getEscalationRisk())
                .summary(analysis.getSummary())
                .sessionStartTime(analysis.getSession() != null ? analysis.getSession().getStartTime() : null)
                .sessionEndTime(analysis.getSession() != null ? analysis.getSession().getEndTime() : null)
                .analyzedAt(analysis.getAnalyzedAt())
                .build();
    }

    /**
     * Calculate sentiment trend based on entries.
     * Compares first half vs second half of timeline.
     * 
     * @param entries List of sentiment entries
     * @return IMPROVING, STABLE, or DECLINING
     */
    private String calculateTrend(List<SentimentTimelineEntryDto> entries) {
        if (entries.isEmpty()) {
            return "STABLE";
        }

        int midpoint = entries.size() / 2;
        if (midpoint == 0) {
            return "STABLE";
        }

        double firstHalfAvg = entries.subList(0, midpoint).stream()
                .mapToDouble(e -> e.getSentimentScore() != null ? e.getSentimentScore() : 0.5)
                .average()
                .orElse(0.5);

        double secondHalfAvg = entries.subList(midpoint, entries.size()).stream()
                .mapToDouble(e -> e.getSentimentScore() != null ? e.getSentimentScore() : 0.5)
                .average()
                .orElse(0.5);

        double diff = secondHalfAvg - firstHalfAvg;
        if (diff > 0.1) {
            return "IMPROVING";
        } else if (diff < -0.1) {
            return "DECLINING";
        }
        return "STABLE";
    }

    /**
     * Calculate average sentiment score.
     * 
     * @param entries List of sentiment entries
     * @return Average score, or null if no entries
     */
    private Double calculateAverageSentiment(List<SentimentTimelineEntryDto> entries) {
        if (entries.isEmpty()) {
            return null;
        }

        return entries.stream()
                .mapToDouble(e -> e.getSentimentScore() != null ? e.getSentimentScore() : 0.5)
                .average()
                .orElse(null);
    }
}
