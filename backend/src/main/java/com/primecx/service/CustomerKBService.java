package com.primecx.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.KBCategorySummaryDto;
import com.primecx.dto.PublicKBArticleDto;
import com.primecx.model.KBArticle;
import com.primecx.model.KBArticleCategory;
import com.primecx.model.KBVisibility;
import com.primecx.repository.KBArticleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Read-only service that powers the customer-facing KB portal.
 *
 * <p>All queries are scoped to published + {@link KBVisibility#PUBLIC} articles so no internal
 * authoring or draft content can be returned, regardless of which id or search term a caller
 * supplies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerKBService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_SEARCH_RESULTS = 25;
    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_QUERY_LENGTH = 100;
    private static final int DEFAULT_RELATED_LIMIT = 5;
    private static final int MAX_RELATED_LIMIT = 10;

    private final KBArticleRepository kbArticleRepository;

    @Transactional(readOnly = true)
    public List<KBCategorySummaryDto> listCategoriesWithCounts() {
        List<KBCategorySummaryDto> out = new ArrayList<>();
        for (KBArticleCategory cat : KBArticleCategory.values()) {
            long count = kbArticleRepository.countByPublishedTrueAndVisibilityAndCategory(
                    KBVisibility.PUBLIC, cat);
            out.add(new KBCategorySummaryDto(cat, count));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<PublicKBArticleDto> listArticles(KBArticleCategory category, int page, int size) {
        Pageable pageable = PageRequest.of(clampPage(page), clampSize(size));
        List<KBArticle> rows = (category == null)
                ? kbArticleRepository.findByPublishedTrueAndVisibilityOrderByUpdatedAtDesc(
                        KBVisibility.PUBLIC, pageable)
                : kbArticleRepository.findByPublishedTrueAndVisibilityAndCategoryOrderByUpdatedAtDesc(
                        KBVisibility.PUBLIC, category, pageable);
        return rows.stream().map(PublicKBArticleDto::summaryOf).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicKBArticleDto> search(String rawQuery) {
        if (rawQuery == null) {
            return List.of();
        }
        String trimmed = rawQuery.trim();
        if (trimmed.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            trimmed = trimmed.substring(0, MAX_QUERY_LENGTH);
        }
        String pattern = "%" + escapeLikePattern(trimmed.toLowerCase()) + "%";
        Pageable pageable = PageRequest.of(0, MAX_SEARCH_RESULTS);
        List<KBArticle> results = kbArticleRepository.searchPublic(
                pattern, KBVisibility.PUBLIC, pageable);
        log.debug("Customer KB search returned {} results", results.size());
        return results.stream().map(PublicKBArticleDto::summaryOf).toList();
    }

    @Transactional(readOnly = true)
    public Optional<PublicKBArticleDto> getArticle(Long id) {
        if (id == null || id < 0) {
            return Optional.empty();
        }
        return kbArticleRepository
                .findByIdAndPublishedTrueAndVisibility(id, KBVisibility.PUBLIC)
                .map(PublicKBArticleDto::of);
    }

    @Transactional(readOnly = true)
    public List<PublicKBArticleDto> getRelatedArticles(Long id, int limit) {
        if (id == null) {
            return List.of();
        }
        Optional<KBArticle> current = kbArticleRepository
                .findByIdAndPublishedTrueAndVisibility(id, KBVisibility.PUBLIC);
        if (current.isEmpty() || current.get().getCategory() == null) {
            return List.of();
        }
        int bounded = Math.min(Math.max(limit, 1), MAX_RELATED_LIMIT);
        Pageable pageable = PageRequest.of(0, bounded);
        List<KBArticle> related = kbArticleRepository
                .findByPublishedTrueAndVisibilityAndCategoryAndIdNotOrderByUpdatedAtDesc(
                        KBVisibility.PUBLIC, current.get().getCategory(), id, pageable);
        return related.stream().map(PublicKBArticleDto::summaryOf).toList();
    }

    static String escapeLikePattern(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' || c == '%' || c == '_') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private int clampPage(int page) {
        return Math.max(page, 0);
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    public static int defaultRelatedLimit() {
        return DEFAULT_RELATED_LIMIT;
    }
}
