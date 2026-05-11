package com.primecx.dto;

import com.primecx.ai.AiPayloadRedactor;

import java.util.List;

/**
 * Staff-facing disclosure of how ticket/session text is prepared before outbound LLM calls.
 * Built from {@link AiPayloadRedactor} and related call sites — keep in sync when flows change.
 */
public record AiLlmDataHandlingDto(
        String overview,
        List<RedactionRule> redactionRules,
        List<TruncationNote> truncationNotes,
        List<LlmFlow> flows) {

    public record RedactionRule(String patternDescription, String replacementPlaceholder) {}

    public record TruncationNote(String description) {}

    public record LlmFlow(
            String flowKey,
            String title,
            boolean payloadRedactionApplied,
            String detail,
            Integer maxCharsSentToProvider) {}

    public static AiLlmDataHandlingDto current() {
        return new AiLlmDataHandlingDto(
                """
                        Before some AI steps, PrimeCX runs deterministic redaction (see rules below). \
                        This reduces accidental email/phone leakage but is not full PII scrubbing — other \
                        sensitive text can still be sent to the model. Always minimize what you paste into AI tools.\
                        """
                        .strip()
                        .replaceAll("\\s+", " "),
                List.of(
                        new RedactionRule(
                                "Email addresses matching a common email-shaped pattern.",
                                AiPayloadRedactor.REDACTED_EMAIL_PLACEHOLDER),
                        new RedactionRule(
                                "Phone-like digit groupings (heuristic; may match some non-phone numbers).",
                                AiPayloadRedactor.REDACTED_PHONE_PLACEHOLDER)),
                List.of(
                        new TruncationNote(
                                "Per-field cap: strings longer than the configured maximum for a flow are cut and end with "
                                        + "`...[truncated]` (see `AiPayloadRedactor.redactAndTruncate`)."),
                        new TruncationNote(
                                "Whole-payload cap: `capTotalLength` appends `...[payload_truncated]` when a total size limit is applied.")),
                List.of(
                        new LlmFlow(
                                "voice_call_ticket_extraction",
                                "Voice / call transcript → structured ticket",
                                true,
                                "Call transcripts are passed through AiPayloadRedactor before the structuring model. "
                                        + "Stored ticket descriptions may include a separate redacted transcript excerpt "
                                        + "(redacted segment capped at "
                                        + AiPayloadRedactor.VOICE_CALL_STORED_DESCRIPTION_TRANSCRIPT_MAX_CHARS
                                        + " characters).",
                                AiPayloadRedactor.VOICE_CALL_STRUCTURE_MODEL_MAX_CHARS),
                        new LlmFlow(
                                "transcript_sentiment_analysis",
                                "Session transcript / notes → sentiment & quality analysis",
                                false,
                                "Uses session notes or the transcript you submit without AiPayloadRedactor.",
                                null),
                        new LlmFlow(
                                "ticket_auto_categorization",
                                "Ticket title + description → category / priority",
                                false,
                                "Sends ticket title and description without AiPayloadRedactor.",
                                null),
                        new LlmFlow(
                                "customer_insight_generation",
                                "Customer ticket and session history → insight JSON",
                                false,
                                "Sends aggregated history text without AiPayloadRedactor.",
                                null)));
    }
}
