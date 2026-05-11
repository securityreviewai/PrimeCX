package com.primecx.dto;

public record TicketCustomerChecklistItemDto(
        String id,
        String label,
        boolean completed
) {}
