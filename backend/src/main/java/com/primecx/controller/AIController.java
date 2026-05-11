package com.primecx.controller;

import com.primecx.dto.*;
import com.primecx.service.AIAnalysisService;
import com.primecx.service.CustomerInsightService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIAnalysisService aiAnalysisService;
    private final CustomerInsightService customerInsightService;

    @PostMapping("/analyze-transcript")
    @PreAuthorize("hasAnyRole('SUPPORT_EXECUTIVE', 'SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<TranscriptAnalysisDto> analyzeTranscript(@RequestBody AnalyzeTranscriptRequest request) {
        log.info("Analyzing transcript for session {}", request.sessionId());
        TranscriptAnalysisDto result = aiAnalysisService.analyzeTranscript(request.sessionId(), request.transcript());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/analysis/session/{sessionId}")
    public ResponseEntity<TranscriptAnalysisDto> getAnalysisForSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(aiAnalysisService.getAnalysisBySessionId(sessionId));
    }

    @GetMapping("/analysis/recent")
    public ResponseEntity<List<TranscriptAnalysisDto>> getRecentAnalyses() {
        return ResponseEntity.ok(aiAnalysisService.getRecentAnalyses());
    }

    @PostMapping("/categorize-ticket/{ticketId}")
    @PreAuthorize("hasAnyRole('SUPPORT_EXECUTIVE', 'SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<TicketCategorizationResult> categorizeTicket(@PathVariable Long ticketId) {
        log.info("Categorizing ticket {}", ticketId);
        TicketCategorizationResult result = aiAnalysisService.categorizeTicket(ticketId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/generate-insights/user/{userId}")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<List<CustomerInsightDto>> generateInsights(@PathVariable Long userId) {
        log.info("Generating AI insights for user {}", userId);
        List<CustomerInsightDto> insights = aiAnalysisService.generateCustomerInsight(userId);
        return ResponseEntity.ok(insights);
    }

    @GetMapping("/insights/user/{userId}")
    public ResponseEntity<List<CustomerInsightDto>> getInsightsForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(customerInsightService.getInsightsForUser(userId));
    }

    @GetMapping("/insights/summary")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<AIInsightsSummary> getInsightsSummary() {
        return ResponseEntity.ok(aiAnalysisService.getInsightsSummary());
    }

    @GetMapping("/insights/recent")
    public ResponseEntity<List<CustomerInsightDto>> getRecentInsights() {
        return ResponseEntity.ok(customerInsightService.getRecentInsights());
    }

    /**
     * What staff should know before using AI features: redaction rules and which flows use them.
     */
    @GetMapping("/llm-data-handling")
    @PreAuthorize("hasAnyRole('SUPPORT_EXECUTIVE', 'SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<AiLlmDataHandlingDto> getLlmDataHandling() {
        return ResponseEntity.ok(AiLlmDataHandlingDto.current());
    }
}
