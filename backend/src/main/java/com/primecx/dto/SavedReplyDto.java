package com.primecx.dto;

import java.time.LocalDateTime;

public record SavedReplyDto(Long id, String title, String body, LocalDateTime createdAt) {}
