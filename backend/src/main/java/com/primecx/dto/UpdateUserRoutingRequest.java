package com.primecx.dto;

import java.util.List;

import com.primecx.model.CustomerTier;

import jakarta.validation.constraints.Size;

/**
 * Partial update of routing-related profile fields. Applied only when the target user's role matches
 * the field (tier for customers, skills for support executives).
 */
public record UpdateUserRoutingRequest(
        CustomerTier customerTier,
        @Size(max = 12) List<@Size(max = 32) String> supportSkills
) {}
