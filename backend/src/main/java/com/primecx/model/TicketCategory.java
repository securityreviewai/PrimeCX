package com.primecx.model;

/**
 * Ticket routing category; matched against support agent {@link User#getSupportSkills()}.
 */
public enum TicketCategory {
    GENERAL,
    BILLING,
    TECHNICAL,
    ACCOUNT,
    PRODUCT
}
