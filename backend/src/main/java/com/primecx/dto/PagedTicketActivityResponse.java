package com.primecx.dto;

import java.util.List;

public record PagedTicketActivityResponse(
        List<TicketActivityDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
