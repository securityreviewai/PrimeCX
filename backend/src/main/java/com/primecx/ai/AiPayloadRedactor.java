package com.primecx.ai;

import java.util.regex.Pattern;

/**
 * Deterministic redaction and size limits before sending text to external LLM APIs.
 */
public final class AiPayloadRedactor {

    /** Max chars of redacted transcript sent to the structuring model (voice / call → ticket flow). */
    public static final int VOICE_CALL_STRUCTURE_MODEL_MAX_CHARS = 14_000;

    /** Max chars of redacted transcript appended into stored ticket description (call pipeline). */
    public static final int VOICE_CALL_STORED_DESCRIPTION_TRANSCRIPT_MAX_CHARS = 12_000;

    public static final String REDACTED_EMAIL_PLACEHOLDER = "[REDACTED_EMAIL]";
    public static final String REDACTED_PHONE_PLACEHOLDER = "[REDACTED_PHONE]";
    public static final String TRUNCATION_SUFFIX_FIELD = "\n...[truncated]";
    public static final String TRUNCATION_SUFFIX_PAYLOAD = "\n...[payload_truncated]";

    private static final Pattern EMAIL = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern PHONE_LIKE = Pattern.compile(
            "\\b(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");

    private AiPayloadRedactor() {
    }

    public static String redactAndTruncate(String input, int maxLen) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String t = EMAIL.matcher(input).replaceAll(REDACTED_EMAIL_PLACEHOLDER);
        t = PHONE_LIKE.matcher(t).replaceAll(REDACTED_PHONE_PLACEHOLDER);
        if (maxLen > 0 && t.length() > maxLen) {
            return t.substring(0, maxLen) + TRUNCATION_SUFFIX_FIELD;
        }
        return t;
    }

    public static String capTotalLength(String payload, int maxTotal) {
        if (payload == null) {
            return "";
        }
        if (maxTotal > 0 && payload.length() > maxTotal) {
            return payload.substring(0, maxTotal) + TRUNCATION_SUFFIX_PAYLOAD;
        }
        return payload;
    }
}
