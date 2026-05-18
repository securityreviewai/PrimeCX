package com.primecx.dto;

import java.util.List;

public record PagedTicketsResponse(
        List<TicketDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
