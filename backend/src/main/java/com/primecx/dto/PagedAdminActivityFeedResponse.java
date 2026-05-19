package com.primecx.dto;

import java.util.List;

public record PagedAdminActivityFeedResponse(
        List<AdminTicketActivityFeedItemDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
