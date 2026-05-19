package com.primecx.model;

public enum TicketActivityType {
    TICKET_CREATED,
    TICKET_UPDATED,
    CLAIMED,
    MESSAGE_POSTED,
    ATTACHMENT_ADDED,
    ATTACHMENT_REMOVED,
    SATISFACTION_SUBMITTED,
    /** Assignee cleared; ticket returned to the unassigned pool (status set to OPEN). */
    RELEASED
}
