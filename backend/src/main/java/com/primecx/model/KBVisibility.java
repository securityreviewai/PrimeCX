package com.primecx.model;

/**
 * Visibility scope for a {@link KBArticle}.
 *
 * <p>INTERNAL_ONLY articles are authored material for support staff and must never be exposed
 * through the customer-facing portal endpoints. PUBLIC articles are eligible to be shown to
 * authenticated customer-portal users (subject also to {@link KBArticle#isPublished()}).
 */
public enum KBVisibility {
    INTERNAL_ONLY,
    PUBLIC
}
