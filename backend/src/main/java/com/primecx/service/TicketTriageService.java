package com.primecx.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.primecx.dto.TicketTriageDto;
import com.primecx.model.Ticket;
import com.primecx.model.TicketCategory;
import com.primecx.model.TicketIntent;

@Service
public class TicketTriageService {

    private static final List<TicketIntent> TIE_BREAK_ORDER = List.of(
            TicketIntent.INCIDENT,
            TicketIntent.ACCESS_ISSUE,
            TicketIntent.BILLING,
            TicketIntent.BUG,
            TicketIntent.FEATURE_REQUEST,
            TicketIntent.HOW_TO,
            TicketIntent.GENERAL
    );

    private static final Map<TicketIntent, IntentProfile> PROFILES = Map.of(
            TicketIntent.BUG,
            new IntentProfile(
                    TicketCategory.TECHNICAL,
                    "engineering-investigation",
                    "Engineering Investigation",
                    "bug-intake",
                    "Bug Reproduction Template",
                    """
                            Thanks for flagging this issue. To speed up investigation, please capture:
                            - The exact steps that reproduce the problem
                            - What you expected to happen vs. what happened instead
                            - The affected page, feature, or environment
                            - Any error text, timestamps, or screenshots you can share
                            """,
                    List.of("intent:bug", "queue:engineering", "needs:reproduction"),
                    List.of("bug", "broken", "error", "exception", "failing", "fails", "failure",
                            "not working", "issue", "defect", "glitch", "crash", "stack trace", "unexpected")
            ),
            TicketIntent.BILLING,
            new IntentProfile(
                    TicketCategory.BILLING,
                    "billing-review",
                    "Billing Review",
                    "billing-resolution",
                    "Billing Investigation Template",
                    """
                            We are reviewing the billing issue. Please confirm:
                            - The invoice, subscription, or order involved
                            - The billing date and amount in question
                            - Whether you expected a refund, credit, or plan change
                            - Any screenshots or transaction references that will help us verify the charge
                            """,
                    List.of("intent:billing", "queue:finance", "needs:charge-review"),
                    List.of("billing", "invoice", "refund", "charged", "charge", "payment",
                            "subscription", "renewal", "credit card", "credit", "plan", "pricing")
            ),
            TicketIntent.ACCESS_ISSUE,
            new IntentProfile(
                    TicketCategory.ACCOUNT,
                    "access-recovery",
                    "Access Recovery",
                    "identity-verification",
                    "Access Recovery Template",
                    """
                            We can help restore access. Please share:
                            - The affected account or workspace
                            - The login method you used (SSO, password, magic link, MFA, etc.)
                            - The exact error or denial message you saw
                            - Whether anything changed recently, such as role updates, password resets, or device changes
                            """,
                    List.of("intent:access-issue", "queue:identity", "needs:verification"),
                    List.of("login", "log in", "sign in", "password", "mfa", "2fa", "otp", "access denied",
                            "forbidden", "permission", "locked out", "unable to access", "cannot access",
                            "account locked", "reset password")
            ),
            TicketIntent.HOW_TO,
            new IntentProfile(
                    TicketCategory.PRODUCT,
                    "guided-how-to",
                    "Guided How-To",
                    "self-service-guidance",
                    "How-To Assistance Template",
                    """
                            Happy to help with the setup. To tailor the guidance, please confirm:
                            - What you are trying to accomplish
                            - Where you got stuck
                            - Any documentation or steps you already tried
                            - Whether this is for a single user, a team, or an admin workflow
                            """,
                    List.of("intent:how-to", "queue:self-service", "type:guidance"),
                    List.of("how to", "how do i", "how can i", "guide", "tutorial", "walk me through",
                            "steps", "documentation", "where can i", "help me configure", "setup")
            ),
            TicketIntent.FEATURE_REQUEST,
            new IntentProfile(
                    TicketCategory.PRODUCT,
                    "product-feedback-loop",
                    "Product Feedback Loop",
                    "feature-request",
                    "Feature Request Template",
                    """
                            Thanks for the suggestion. To route it to product review, please include:
                            - The workflow or outcome you want to unlock
                            - Who would benefit from the change
                            - Any workaround you use today
                            - Why the current product behavior is not sufficient
                            """,
                    List.of("intent:feature-request", "queue:product", "type:enhancement"),
                    List.of("feature request", "feature", "enhancement", "would like", "wishlist", "can you add",
                            "request an option", "missing capability")
            ),
            TicketIntent.INCIDENT,
            new IntentProfile(
                    TicketCategory.TECHNICAL,
                    "incident-assessment",
                    "Incident Assessment",
                    "incident-response",
                    "Incident Impact Template",
                    """
                            We are assessing the incident impact. Please capture:
                            - When the issue started and whether it is still active
                            - How many users or workflows are affected
                            - Any visible error rates, outages, or degraded behavior
                            - Whether you have a workaround or recovery signal already
                            """,
                    List.of("intent:incident", "queue:incident", "severity:possible-outage"),
                    List.of("outage", "down", "degraded", "latency", "service unavailable", "503",
                            "500", "all users", "major incident", "widespread", "out of service", "slow")
            ),
            TicketIntent.GENERAL,
            new IntentProfile(
                    TicketCategory.GENERAL,
                    "general-triage",
                    "General Triage",
                    "general-follow-up",
                    "General Follow-up Template",
                    """
                            Thanks for reaching out. To help us triage this quickly, please share:
                            - The goal you were trying to accomplish
                            - What happened when you attempted it
                            - Any timestamps, screenshots, or examples that illustrate the issue
                            - Whether this affects only you or a broader group
                            """,
                    List.of("intent:general", "queue:triage"),
                    List.of()
            )
    );

    public TicketTriageDecision triage(String title, String description) {
        String normalizedTitle = normalize(title);
        String normalizedBody = normalize(description);
        String combined = (normalizedTitle + " " + normalizedBody).trim();

        Map<TicketIntent, ScoreCard> scored = new EnumMap<>(TicketIntent.class);
        for (TicketIntent intent : TicketIntent.values()) {
            IntentProfile profile = PROFILES.get(intent);
            ScoreCard card = new ScoreCard();
            for (String keyword : profile.keywords()) {
                int hit = scoreKeyword(normalizedTitle, normalizedBody, keyword);
                if (hit > 0) {
                    card.score += hit;
                    card.matches.add(keyword);
                }
            }
            scored.put(intent, card);
        }

        if (containsAny(combined, List.of("can't login", "cannot login", "unable to login", "access denied"))) {
            boost(scored, TicketIntent.ACCESS_ISSUE, 5, "login recovery");
        }
        if (containsAny(combined, List.of("how do i", "how to", "walk me through"))) {
            boost(scored, TicketIntent.HOW_TO, 4, "guided setup");
        }
        if (containsAny(combined, List.of("all users", "everyone", "entire team", "whole company"))) {
            boost(scored, TicketIntent.INCIDENT, 4, "broad impact");
        }
        if (containsAny(combined, List.of("refund", "double charged", "charged twice"))) {
            boost(scored, TicketIntent.BILLING, 4, "billing dispute");
        }

        TicketIntent selected = scored.entrySet().stream()
                .max(Comparator
                        .<Map.Entry<TicketIntent, ScoreCard>>comparingInt(entry -> entry.getValue().score)
                        .thenComparingInt(entry -> -TIE_BREAK_ORDER.indexOf(entry.getKey())))
                .map(Map.Entry::getKey)
                .orElse(TicketIntent.GENERAL);

        if (scored.get(selected).score <= 0) {
            selected = TicketIntent.GENERAL;
        }

        IntentProfile profile = PROFILES.get(selected);
        ScoreCard primary = scored.get(selected);
        TicketIntent selectedIntent = selected;
        int secondBest = scored.entrySet().stream()
                .filter(entry -> entry.getKey() != selectedIntent)
                .mapToInt(entry -> entry.getValue().score)
                .max()
                .orElse(0);

        double confidence = primary.score <= 0
                ? 0.35
                : Math.min(0.97, Math.max(0.40, (double) primary.score / (double) (primary.score + secondBest + 2)));

        String rationale = primary.matches.isEmpty()
                ? "No strong intent keywords were detected, so the ticket stayed in general triage."
                : "Matched intent signals: " + String.join(", ", primary.matches.stream().distinct().limit(3).toList()) + ".";

        return new TicketTriageDecision(
                selected,
                profile.category(),
                List.copyOf(profile.tags()),
                profile.workflowKey(),
                profile.workflowLabel(),
                profile.templateKey(),
                profile.templateTitle(),
                profile.templateBody(),
                confidence,
                rationale
        );
    }

    public void apply(Ticket ticket) {
        TicketTriageDecision triage = triage(ticket.getTitle(), ticket.getDescription());
        ticket.setIntent(triage.intent());
        ticket.setCategory(triage.category());
        ticket.setTags(new LinkedHashSet<>(triage.tags()));
        ticket.setWorkflowKey(triage.workflowKey());
        ticket.setTemplateKey(triage.templateKey());
        ticket.setTriageConfidence(triage.confidence());
        ticket.setTriageRationale(triage.rationale());
    }

    public TicketTriageDto toDto(Ticket ticket) {
        if (ticket.getIntent() == null
                && ticket.getCategory() == null
                && (ticket.getTags() == null || ticket.getTags().isEmpty())
                && ticket.getWorkflowKey() == null
                && ticket.getTemplateKey() == null) {
            return null;
        }

        TicketIntent intent = ticket.getIntent() != null ? ticket.getIntent() : TicketIntent.GENERAL;
        IntentProfile profile = PROFILES.getOrDefault(intent, PROFILES.get(TicketIntent.GENERAL));

        return new TicketTriageDto(
                intent,
                ticket.getCategory() != null ? ticket.getCategory() : profile.category(),
                ticket.getTags() == null ? List.of() : ticket.getTags().stream().sorted().toList(),
                ticket.getWorkflowKey() != null ? ticket.getWorkflowKey() : profile.workflowKey(),
                profile.workflowLabel(),
                ticket.getTemplateKey() != null ? ticket.getTemplateKey() : profile.templateKey(),
                profile.templateTitle(),
                profile.templateBody(),
                ticket.getTriageConfidence(),
                ticket.getTriageRationale()
        );
    }

    private static void boost(Map<TicketIntent, ScoreCard> scored, TicketIntent intent, int amount, String label) {
        ScoreCard card = scored.get(intent);
        card.score += amount;
        card.matches.add(label);
    }

    private static int scoreKeyword(String normalizedTitle, String normalizedBody, String keyword) {
        String normalizedKeyword = normalize(keyword);
        int score = 0;
        if (!normalizedTitle.isBlank() && normalizedTitle.contains(normalizedKeyword)) {
            score += 3;
        }
        if (!normalizedBody.isBlank() && normalizedBody.contains(normalizedKeyword)) {
            score += 2;
        }
        return score;
    }

    private static boolean containsAny(String haystack, List<String> needles) {
        if (haystack == null || haystack.isBlank()) {
            return false;
        }
        for (String needle : needles) {
            if (haystack.contains(normalize(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record IntentProfile(
            TicketCategory category,
            String workflowKey,
            String workflowLabel,
            String templateKey,
            String templateTitle,
            String templateBody,
            List<String> tags,
            List<String> keywords
    ) {}

    private static final class ScoreCard {
        private int score;
        private final List<String> matches = new ArrayList<>();
    }

    public record TicketTriageDecision(
            TicketIntent intent,
            TicketCategory category,
            List<String> tags,
            String workflowKey,
            String workflowLabel,
            String templateKey,
            String templateTitle,
            String templateBody,
            double confidence,
            String rationale
    ) {}
}
