package com.primecx.model;

import java.util.Locale;

/**
 * Aligns with AI suggested_category slugs from {@link com.primecx.service.AIAnalysisService}.
 */
public enum TicketCategory {
    BILLING,
    TECHNICAL,
    ACCOUNT,
    PRODUCT_FEEDBACK,
    SERVICE_OUTAGE,
    GENERAL_INQUIRY,
    COMPLAINT,
    FEATURE_REQUEST;

    /** Maps AI / URL slug ("billing", "product_feedback") to enum; falls back to {@link #GENERAL_INQUIRY}. */
    public static TicketCategory fromAiSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return GENERAL_INQUIRY;
        }
        String key = slug.strip().toLowerCase(Locale.ROOT).replace('-', '_');
        try {
            return TicketCategory.valueOf(key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return GENERAL_INQUIRY;
        }
    }
}
