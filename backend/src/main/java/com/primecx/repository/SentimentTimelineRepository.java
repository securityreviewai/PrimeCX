package com.primecx.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.primecx.model.TranscriptAnalysis;
import java.time.LocalDateTime;

/**
 * Repository for TranscriptAnalysis with scoped queries.
 * Enforces BOLA prevention by querying with customer/tenant scope (guardrail 136).
 * Uses parameterized queries to prevent SQL injection (guardrails 126, 114, 199).
 */
@Repository
public interface SentimentTimelineRepository extends JpaRepository<TranscriptAnalysis, Long> {

    /**
     * Fetch sentiment timeline for a customer within date range.
     * Scoped query to prevent BOLA - only returns data for sessions belonging to the specified customer.
     * Uses parameterized queries to prevent SQL injection.
     * 
     * @param customerId The customer (user) ID
     * @param startDate Start of date range (can be null)
     * @param endDate End of date range (can be null)
     * @param pageable Pagination info
     * @return Page of sentiment analyses for the customer
     */
    @Query("SELECT ta FROM TranscriptAnalysis ta " +
           "WHERE ta.session.user.id = :customerId " +
           "AND (:startDate IS NULL OR ta.analyzedAt >= :startDate) " +
           "AND (:endDate IS NULL OR ta.analyzedAt <= :endDate) " +
           "ORDER BY ta.analyzedAt DESC")
    Page<TranscriptAnalysis> findCustomerSentimentTimeline(
        @Param("customerId") Long customerId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * Count sentiment entries for a customer in date range.
     * Used for calculating trend analysis.
     * 
     * @param customerId The customer ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of sentiment analyses
     */
    @Query("SELECT COUNT(ta) FROM TranscriptAnalysis ta " +
           "WHERE ta.session.user.id = :customerId " +
           "AND ta.analyzedAt >= :startDate " +
           "AND ta.analyzedAt <= :endDate")
    Long countCustomerSentimentsInRange(
        @Param("customerId") Long customerId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find sentiment entries with escalation risks for a customer.
     * Used to detect escalation patterns.
     * 
     * @param customerId The customer ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Count of escalation risks
     */
    @Query("SELECT COUNT(ta) FROM TranscriptAnalysis ta " +
           "WHERE ta.session.user.id = :customerId " +
           "AND ta.analyzedAt >= :startDate " +
           "AND ta.analyzedAt <= :endDate " +
           "AND ta.escalationRisk IS NOT NULL " +
           "AND ta.escalationRisk != ''")
    Long countEscalationRisks(
        @Param("customerId") Long customerId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
