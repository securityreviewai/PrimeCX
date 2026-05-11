package com.primecx.dto;

import com.primecx.model.KBArticleCategory;

/**
 * Summary of a KB category for the customer portal navigation: the category enum value and
 * the number of published + PUBLIC articles it currently contains.
 */
public record KBCategorySummaryDto(KBArticleCategory category, long articleCount) {
}
