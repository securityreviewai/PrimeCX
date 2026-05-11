package com.primecx.dto;

import jakarta.validation.constraints.NotNull;

public record SetLegalHoldRequest(@NotNull Boolean legalHold) {}
