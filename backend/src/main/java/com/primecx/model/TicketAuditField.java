package com.primecx.model;

/**
 * Fields recorded in {@link com.primecx.model.TicketChangeLog} (append-only audit rows).
 */
public enum TicketAuditField {
    STATUS,
    PRIORITY,
    ASSIGNED_TO,
    ESTIMATED_RESOLUTION,
    ETA_TRANSPARENCY_NOTE
}
