package com.primecx.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.primecx.dto.AIInsightsSummary;
import com.primecx.dto.CustomerInsightDto;
import com.primecx.dto.TicketCategorizationResult;
import com.primecx.dto.TranscriptAnalysisDto;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.*;
import com.primecx.repository.CustomerInsightRepository;
import com.primecx.repository.TranscriptAnalysisRepository;
import com.primecx.repository.SupportSessionRepository;
import com.primecx.repository.TicketRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AIAnalysisService {

    private final WebClient openAiWebClient;
    private final String modelName;
    private final ObjectMapper objectMapper;
    private final TranscriptAnalysisRepository transcriptAnalysisRepository;
    private final CustomerInsightRepository customerInsightRepository;
    private final SupportSessionRepository supportSessionRepository;
    private final TicketRepository ticketRepository;

    public AIAnalysisService(
            @Qualifier("openAiWebClient") WebClient openAiWebClient,
            @Qualifier("openAiModel") String modelName,
            ObjectMapper objectMapper,
            TranscriptAnalysisRepository transcriptAnalysisRepository,
            CustomerInsightRepository customerInsightRepository,
            SupportSessionRepository supportSessionRepository,
            TicketRepository ticketRepository) {
        this.openAiWebClient = openAiWebClient;
        this.modelName = modelName;
        this.objectMapper = objectMapper;
        this.transcriptAnalysisRepository = transcriptAnalysisRepository;
        this.customerInsightRepository = customerInsightRepository;
        this.supportSessionRepository = supportSessionRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public TranscriptAnalysisDto analyzeTranscript(Long sessionId, String transcript) {
        SupportSession session = supportSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportSession", sessionId));

        String textToAnalyze = (transcript != null && !transcript.isBlank()) ? transcript : session.getNotes();
        if (textToAnalyze == null || textToAnalyze.isBlank()) {
            throw new IllegalArgumentException("No transcript or session notes available for analysis");
        }

        TranscriptAnalysis analysis = TranscriptAnalysis.builder()
                .session(session)
                .status(AnalysisStatus.PENDING)
                .build();
        analysis = transcriptAnalysisRepository.save(analysis);

        try {
            analysis.setStatus(AnalysisStatus.PROCESSING);
            analysis = transcriptAnalysisRepository.save(analysis);

            String systemPrompt = buildTranscriptAnalysisPrompt();
            String response = callOpenAI(systemPrompt, textToAnalyze);
            analysis.setRawAiResponse(response);

            JsonNode root = objectMapper.readTree(response);

            JsonNode sentimentNode = root.path("sentiment");
            String sentimentTypeStr = sentimentNode.path("type").asText("NEUTRAL").toUpperCase();
            analysis.setSentimentType(SentimentType.valueOf(sentimentTypeStr));
            analysis.setSentimentScore(sentimentNode.path("score").asDouble(0.0));

            analysis.setSummary(root.path("summary").asText());
            analysis.setKeyTopics(objectMapper.writeValueAsString(root.path("key_topics")));
            analysis.setCustomerSatisfactionScore(root.path("customer_satisfaction_score").asDouble(3.0));
            analysis.setResolutionQuality(root.path("resolution_quality").asText("unresolved"));
            analysis.setSuggestedFollowUps(objectMapper.writeValueAsString(root.path("suggested_follow_ups")));
            analysis.setEscalationRisk(root.path("escalation_risk").asText("medium"));

            analysis.setStatus(AnalysisStatus.COMPLETED);
            analysis.setAnalyzedAt(LocalDateTime.now());

            log.info("Transcript analysis completed for session {}", sessionId);
        } catch (Exception e) {
            log.error("Transcript analysis failed for session {}: {}", sessionId, e.getMessage(), e);
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setErrorMessage(e.getMessage());
        }

        analysis = transcriptAnalysisRepository.save(analysis);
        return toDto(analysis);
    }

    @Transactional
    public TicketCategorizationResult categorizeTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        String systemPrompt = """
                You are a customer support ticket classification expert. Analyze the ticket and return a JSON object with:
                - "suggested_category": one of ["billing", "technical", "account", "product_feedback", "service_outage", "general_inquiry", "complaint", "feature_request"]
                - "suggested_priority": one of ["LOW", "MEDIUM", "HIGH", "CRITICAL"]
                - "reasoning": a brief explanation of your classification rationale
                - "similar_ticket_ids": an empty array (placeholder for future similarity matching)
                
                Consider urgency signals, customer impact, and issue complexity when assessing priority.\
                """;

        String userMessage = "Ticket Title: " + ticket.getTitle() + "\n\nTicket Description: " + ticket.getDescription();

        try {
            String response = callOpenAI(systemPrompt, userMessage);
            JsonNode root = objectMapper.readTree(response);

            List<Long> similarIds = objectMapper.convertValue(
                    root.path("similar_ticket_ids"),
                    new TypeReference<List<Long>>() {});

            return new TicketCategorizationResult(
                    ticketId,
                    root.path("suggested_category").asText(),
                    root.path("suggested_priority").asText(),
                    root.path("reasoning").asText(),
                    similarIds != null ? similarIds : List.of()
            );
        } catch (Exception e) {
            log.error("Ticket categorization failed for ticket {}: {}", ticketId, e.getMessage(), e);
            return new TicketCategorizationResult(ticketId, "general_inquiry", "MEDIUM",
                    "Categorization failed: " + e.getMessage(), List.of());
        }
    }

    @Transactional
    public List<CustomerInsightDto> generateCustomerInsight(Long userId) {
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        List<SupportSession> sessions = supportSessionRepository.findByUserId(userId);

        StringBuilder historyBuilder = new StringBuilder();
        historyBuilder.append("CUSTOMER TICKET HISTORY:\n");
        for (Ticket t : tickets) {
            historyBuilder.append(String.format("- [%s] %s (Status: %s, Priority: %s)\n  %s\n",
                    t.getCreatedAt(), t.getTitle(), t.getStatus(), t.getPriority(),
                    t.getDescription() != null ? t.getDescription() : "No description"));
        }

        historyBuilder.append("\nSUPPORT SESSION HISTORY:\n");
        for (SupportSession s : sessions) {
            historyBuilder.append(String.format("- Session %d (Status: %s, %s to %s)\n  Notes: %s\n",
                    s.getId(), s.getStatus(), s.getStartTime(), s.getEndTime(),
                    s.getNotes() != null ? s.getNotes() : "No notes"));
        }

        String systemPrompt = """
                You are a customer experience intelligence analyst for a support platform. Given a customer's complete \
                interaction history (tickets and support sessions), generate actionable insights. Return a JSON object with:
                - "insights": an array of objects, each containing:
                  - "type": one of ["sentiment_trend", "issue_pattern", "churn_risk", "satisfaction_summary"]
                  - "title": a concise insight title
                  - "description": a 2-3 sentence narrative explanation
                  - "data": a JSON object with structured metrics relevant to the insight type
                  - "confidence_score": a value between 0.0 and 1.0
                
                Generate exactly one insight per type. For sentiment_trend, track how the customer's tone evolved. \
                For issue_pattern, identify recurring problems. For churn_risk, assess likelihood of customer departure \
                based on frustration signals and unresolved issues. For satisfaction_summary, provide an overall assessment.\
                """;

        List<CustomerInsight> savedInsights = new ArrayList<>();

        try {
            String response = callOpenAI(systemPrompt, historyBuilder.toString());
            JsonNode root = objectMapper.readTree(response);
            JsonNode insightsArray = root.path("insights");

            User user = sessions.isEmpty()
                    ? tickets.isEmpty() ? null : tickets.get(0).getUser()
                    : sessions.get(0).getUser();

            if (user == null) {
                throw new ResourceNotFoundException("User", userId);
            }

            for (JsonNode insightNode : insightsArray) {
                CustomerInsight insight = CustomerInsight.builder()
                        .user(user)
                        .insightType(insightNode.path("type").asText())
                        .title(insightNode.path("title").asText())
                        .description(insightNode.path("description").asText())
                        .data(objectMapper.writeValueAsString(insightNode.path("data")))
                        .confidenceScore(insightNode.path("confidence_score").asDouble(0.5))
                        .validUntil(LocalDateTime.now().plusDays(7))
                        .build();
                savedInsights.add(customerInsightRepository.save(insight));
            }

            log.info("Generated {} insights for user {}", savedInsights.size(), userId);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Customer insight generation failed for user {}: {}", userId, e.getMessage(), e);
        }

        return savedInsights.stream().map(this::toInsightDto).toList();
    }

    public TranscriptAnalysisDto getAnalysisBySessionId(Long sessionId) {
        TranscriptAnalysis analysis = transcriptAnalysisRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("TranscriptAnalysis for session", sessionId));
        return toDto(analysis);
    }

    public List<TranscriptAnalysisDto> getRecentAnalyses() {
        return transcriptAnalysisRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    public AIInsightsSummary getInsightsSummary() {
        Double overallSentiment = transcriptAnalysisRepository.findAverageSentimentScore().orElse(0.0);
        long totalAnalyses = transcriptAnalysisRepository.count();
        Double avgSatisfaction = transcriptAnalysisRepository.findAverageCustomerSatisfactionScore().orElse(0.0);

        Map<String, Long> sentimentDistribution = new LinkedHashMap<>();
        for (SentimentType type : SentimentType.values()) {
            sentimentDistribution.put(type.name(), transcriptAnalysisRepository.countBySentimentType(type));
        }

        List<TranscriptAnalysis> recentAnalyses = transcriptAnalysisRepository.findTop10ByOrderByCreatedAtDesc();
        Map<String, Long> escalationBreakdown = recentAnalyses.stream()
                .filter(a -> a.getEscalationRisk() != null)
                .collect(Collectors.groupingBy(TranscriptAnalysis::getEscalationRisk, Collectors.counting()));

        List<String> topCategories = recentAnalyses.stream()
                .filter(a -> a.getKeyTopics() != null)
                .flatMap(a -> parseJsonArray(a.getKeyTopics()).stream())
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        List<CustomerInsightDto> recentInsights = customerInsightRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toInsightDto)
                .toList();

        return new AIInsightsSummary(
                overallSentiment,
                totalAnalyses,
                avgSatisfaction,
                topCategories,
                escalationBreakdown,
                recentInsights,
                sentimentDistribution
        );
    }

    private String callOpenAI(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.3
        );

        String responseBody = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode responseJson = objectMapper.readTree(responseBody);
            return responseJson
                    .path("choices").get(0)
                    .path("message")
                    .path("content")
                    .asText();
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }

    private List<String> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON array: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildTranscriptAnalysisPrompt() {
        return """
                You are a senior customer experience analyst specializing in support interaction quality assessment. \
                Your task is to perform a comprehensive analysis of a customer support session transcript or notes.
                
                Analyze the provided text and evaluate the following dimensions thoroughly:
                
                1. **Sentiment**: Determine the overall emotional tone of the interaction. Consider both the customer's \
                expressed emotions and the trajectory of sentiment throughout the conversation. Classify as POSITIVE, \
                NEGATIVE, NEUTRAL, or MIXED, and provide a numerical score from -1.0 (extremely negative) to 1.0 \
                (extremely positive).
                
                2. **Summary**: Write a concise yet comprehensive summary (2-4 sentences) capturing the core issue, \
                key discussion points, actions taken by the support executive, and the outcome.
                
                3. **Key Topics**: Extract the primary subjects discussed, including product areas, technical components, \
                specific features, or business processes mentioned. Return as an array of short descriptive strings.
                
                4. **Customer Satisfaction Score**: Predict the customer's likely satisfaction rating on a 1-5 scale \
                based on tone, issue resolution, response quality, and empathy demonstrated by the support executive.
                
                5. **Resolution Quality**: Assess whether the customer's issue was "fully_resolved", "partially_resolved", \
                or "unresolved" based on evidence in the conversation.
                
                6. **Suggested Follow-ups**: Recommend specific, actionable next steps to improve the customer relationship \
                or address remaining concerns. Return as an array of action items.
                
                7. **Escalation Risk**: Evaluate the likelihood that this customer will escalate or churn. Classify as \
                "low", "medium", or "high" based on frustration signals, unresolved issues, and repeated contacts.
                
                Return your analysis as a JSON object with this exact structure:
                {
                  "sentiment": { "type": "POSITIVE|NEGATIVE|NEUTRAL|MIXED", "score": <float> },
                  "summary": "<string>",
                  "key_topics": ["<string>", ...],
                  "customer_satisfaction_score": <float 1.0-5.0>,
                  "resolution_quality": "fully_resolved|partially_resolved|unresolved",
                  "suggested_follow_ups": ["<string>", ...],
                  "escalation_risk": "low|medium|high"
                }\
                """;
    }

    public TranscriptAnalysisDto toDto(TranscriptAnalysis analysis) {
        SupportSession session = analysis.getSession();
        String executiveName = session.getSupportExecutive().getFirstName() + " "
                + session.getSupportExecutive().getLastName();

        return new TranscriptAnalysisDto(
                analysis.getId(),
                session.getId(),
                session.getTicket().getId(),
                session.getTicket().getTitle(),
                executiveName,
                analysis.getSentimentType(),
                analysis.getSentimentScore(),
                analysis.getSummary(),
                parseJsonArray(analysis.getKeyTopics()),
                analysis.getCustomerSatisfactionScore(),
                analysis.getResolutionQuality(),
                parseJsonArray(analysis.getSuggestedFollowUps()),
                analysis.getEscalationRisk(),
                analysis.getStatus(),
                analysis.getAnalyzedAt()
        );
    }

    private CustomerInsightDto toInsightDto(CustomerInsight insight) {
        User user = insight.getUser();
        String userName = user.getFirstName() + " " + user.getLastName();
        return new CustomerInsightDto(
                insight.getId(),
                user.getId(),
                userName,
                insight.getInsightType(),
                insight.getTitle(),
                insight.getDescription(),
                insight.getData(),
                insight.getConfidenceScore(),
                insight.getValidUntil(),
                insight.getCreatedAt()
        );
    }
}
