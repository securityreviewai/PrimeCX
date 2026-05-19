package com.primecx.dto;

public record BatchTicketAssignResultDto(
        int assigned,
        int skippedNotFound,
        int skippedClosedOrResolved,
        int skippedAlreadyAssigned
) {
}
