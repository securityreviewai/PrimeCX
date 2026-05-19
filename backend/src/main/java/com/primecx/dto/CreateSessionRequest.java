package com.primecx.dto;

import jakarta.validation.constraints.NotNull;

public record CreateSessionRequest(
        @NotNull Long ticketId,
        Long userId
) {}
