package com.primecx.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.KBCategorySummaryDto;
import com.primecx.dto.PublicKBArticleDto;
import com.primecx.model.KBArticleCategory;
import com.primecx.service.CustomerKBService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Customer-facing KB portal endpoints. All routes are read-only and return projections that
 * never expose internal authoring metadata. Every endpoint requires an authenticated principal
 * and delegates the visibility/publish filtering to {@link CustomerKBService}.
 */
@Slf4j
@RestController
@RequestMapping("/api/portal/kb")
@PreAuthorize("isAuthenticated()")
@Validated
@RequiredArgsConstructor
public class CustomerKBController {

    private final CustomerKBService customerKBService;

    @GetMapping("/categories")
    public ResponseEntity<List<KBCategorySummaryDto>> listCategories() {
        return ResponseEntity.ok(customerKBService.listCategoriesWithCounts());
    }

    @GetMapping("/articles")
    public ResponseEntity<List<PublicKBArticleDto>> listArticles(
            @RequestParam(required = false) KBArticleCategory category,
            @RequestParam(required = false, defaultValue = "0") @Min(0) @Max(1000) int page,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok(customerKBService.listArticles(category, page, size));
    }

    @GetMapping("/articles/search")
    public ResponseEntity<List<PublicKBArticleDto>> search(
            @RequestParam("q") @Size(min = 1, max = 100) String q) {
        return ResponseEntity.ok(customerKBService.search(q));
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<PublicKBArticleDto> getArticle(@PathVariable Long id) {
        return customerKBService.getArticle(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/articles/{id}/related")
    public ResponseEntity<List<PublicKBArticleDto>> getRelatedArticles(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "5") @Min(1) @Max(10) int limit) {
        return ResponseEntity.ok(customerKBService.getRelatedArticles(id, limit));
    }
}
