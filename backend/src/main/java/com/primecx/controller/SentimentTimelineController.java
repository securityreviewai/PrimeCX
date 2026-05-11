package com.primecx.controller;

import com.primecx.dto.SentimentTimelineRequest;
import com.primecx.dto.SentimentTimelineResponse;
import com.primecx.service.SentimentTimelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import com.primecx.model.User;
import com.primecx.repository.UserRepository;

/**
 * REST Controller for sentiment timeline endpoints.
 * 
 * Enforces security guardrails:
 * - 128: Request DTOs validated with @Valid
 * - 120: Returns safe error messages without verbose details
 * - 121: Service logs all sentiment data access
 * - 134, 139: Authorization checks in service layer
 */
@Slf4j
@RestController
@RequestMapping("/api/sentiment-timeline")
@RequiredArgsConstructor
public class SentimentTimelineController {

    private final SentimentTimelineService sentimentTimelineService;
    private final UserRepository userRepository;

    /**
     * Get customer sentiment timeline.
     * 
     * Endpoint: GET /api/sentiment-timeline
     * 
     * Query Parameters:
     * - customerId (required): Customer ID
     * - startDate (optional): Start date for range
     * - endDate (optional): End date for range
     * - page (optional): Page number (0-indexed), default 0
     * - pageSize (optional): Page size (1-100), default 20
     * 
     * Security:
     * - Requires authentication (enforced by SecurityConfig)
     * - Authorization: User can only view their own timeline or support staff can view assigned customers
     * - Input validation: All parameters validated via @Valid
     * - Returns: No sensitive internal data, only necessary fields
     * 
     * @param request Validated sentiment timeline request
     * @param principal The authenticated user (OIDC)
     * @return ResponseEntity with sentiment timeline or error
     */
    @GetMapping
    public ResponseEntity<SentimentTimelineResponse> getSentimentTimeline(
            @Valid SentimentTimelineRequest request,
            @AuthenticationPrincipal OidcUser principal) {

        try {
            // Resolve current user from JWT
            String userEmail = principal.getEmail();
            User currentUser = userRepository.findByEmail(userEmail)
                    .orElse(null);

            if (currentUser == null) {
                log.warn("SECURITY: User not found in database: {}", userEmail);
                // Gardrail 120: Don't expose that user wasn't found - generic error
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .build();
            }

            // Call service with authorization checks
            SentimentTimelineResponse response = sentimentTimelineService
                    .getSentimentTimeline(request, currentUser.getId());

            return ResponseEntity.ok(response);

        } catch (org.springframework.security.access.AccessDeniedException e) {
            log.warn("SECURITY: Authorization denied for sentiment timeline access: {}", 
                    principal.getEmail());
            // Gardrail 120: Generic error message
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .build();

        } catch (IllegalArgumentException e) {
            log.warn("SECURITY: Invalid request parameters for sentiment timeline: {}", 
                    e.getMessage());
            // Return safe error message
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .build();

        } catch (Exception e) {
            log.error("ERROR: Unexpected error retrieving sentiment timeline", e);
            // Gardrail 120: Don't expose stack trace or internal errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    /**
     * Check if customer has escalation risks.
     * 
     * Endpoint: GET /api/sentiment-timeline/{customerId}/escalation-risks
     * 
     * Query Parameters:
     * - lookbackDays (optional): How many days to look back, default 30
     * 
     * @param customerId Customer ID to check
     * @param lookbackDays Optional lookback period in days
     * @param principal The authenticated user
     * @return true if escalation risks found, false otherwise
     */
    @GetMapping("/{customerId}/escalation-risks")
    public ResponseEntity<Boolean> hasEscalationRisks(
            @PathVariable Long customerId,
            @RequestParam(value = "lookbackDays", required = false) Integer lookbackDays,
            @AuthenticationPrincipal OidcUser principal) {

        try {
            String userEmail = principal.getEmail();
            User currentUser = userRepository.findByEmail(userEmail)
                    .orElse(null);

            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .build();
            }

            Boolean hasRisks = sentimentTimelineService.hasEscalationRisks(
                    customerId, lookbackDays);

            // Log escalation risk checks for audit trail
            log.info("SECURITY_AUDIT: Escalation risk check - customerId={}, by user={}, hasRisks={}", 
                    customerId, userEmail, hasRisks);

            return ResponseEntity.ok(hasRisks);

        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .build();

        } catch (Exception e) {
            log.error("ERROR: Unexpected error checking escalation risks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}
