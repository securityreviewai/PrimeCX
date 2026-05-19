package com.primecx.dto;

/** Snapshot of how much each support executive is carrying in queue + live sessions. */
public record ExecutiveWorkloadDto(
        Long executiveId,
        String executiveName,
        long activeQueueTickets,
        long activeSessions
) {
}
