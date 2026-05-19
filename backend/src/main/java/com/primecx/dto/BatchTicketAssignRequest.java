package com.primecx.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BatchTicketAssignRequest(
        @NotEmpty
        @Size(max = 100)
        List<Long> ticketIds,
        @NotNull
        Long assigneeUserId
) {
}
