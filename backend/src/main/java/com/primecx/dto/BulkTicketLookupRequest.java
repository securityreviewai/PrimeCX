package com.primecx.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BulkTicketLookupRequest(
        @NotEmpty
        @Size(max = 100)
        List<Long> ticketIds
) {
}
