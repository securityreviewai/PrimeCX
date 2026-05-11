package com.primecx.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.primecx.model.KBArticle;
import com.primecx.model.KBArticleCategory;

/**
 * Projection of a {@link KBArticle} that is safe to return to customer-portal readers.
 *
 * <p>Deliberately omits authoring metadata (createdBy, updatedBy) and the INTERNAL_ONLY visibility
 * flag so that even if an internal-only article were mis-queried, no authoring attribution would
 * leak through this DTO.
 */
public record PublicKBArticleDto(
        Long id,
        String title,
        String content,
        KBArticleCategory category,
        List<String> tags,
        LocalDateTime updatedAt) {

    public static PublicKBArticleDto of(KBArticle article) {
        List<String> parsedTags = List.of();
        if (article.getTags() != null && !article.getTags().isBlank()) {
            parsedTags = java.util.Arrays.stream(article.getTags().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        LocalDateTime ts = article.getUpdatedAt() != null ? article.getUpdatedAt() : article.getCreatedAt();
        return new PublicKBArticleDto(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getCategory(),
                parsedTags,
                ts);
    }

    /**
     * Variant without body content, used for list views where emitting full content would waste
     * bandwidth and increase exposure surface.
     */
    public static PublicKBArticleDto summaryOf(KBArticle article) {
        PublicKBArticleDto full = of(article);
        return new PublicKBArticleDto(
                full.id(),
                full.title(),
                null,
                full.category(),
                full.tags(),
                full.updatedAt());
    }
}
