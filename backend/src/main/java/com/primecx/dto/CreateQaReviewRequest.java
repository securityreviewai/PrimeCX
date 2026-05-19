package com.primecx.dto;

public record CreateQaReviewRequest(
        Integer empathyScore,
        Integer accuracyScore,
        Integer complianceScore,
        String notes
) {}
