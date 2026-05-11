package com.primecx.controller;

import com.primecx.dto.CreateKBArticleRequest;
import com.primecx.model.KBArticle;
import com.primecx.service.KBArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/kb")
@Validated
public class KBController {

    @Autowired
    private KBArticleService kbArticleService;

    @GetMapping("/search")
    @PreAuthorize("hasRole('ROLE_SUPPORT_EXECUTIVE') or hasRole('ROLE_SUPPORT_ADMIN') or hasRole('ROLE_SUPPORT_MANAGER')")
    public ResponseEntity<List<KBArticle>> searchArticles(@RequestParam String q) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<KBArticle> results = kbArticleService.searchArticles(q);
        return ResponseEntity.ok(results);
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_SUPPORT_EXECUTIVE') or hasRole('ROLE_SUPPORT_ADMIN') or hasRole('ROLE_SUPPORT_MANAGER')")
    public ResponseEntity<List<KBArticle>> getAllArticles() {
        List<KBArticle> articles = kbArticleService.getAllArticles();
        return ResponseEntity.ok(articles);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPPORT_EXECUTIVE') or hasRole('ROLE_SUPPORT_ADMIN') or hasRole('ROLE_SUPPORT_MANAGER')")
    public ResponseEntity<KBArticle> getArticle(@PathVariable Long id) {
        KBArticle article = kbArticleService.getArticleById(id);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(article);
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_SUPPORT_EXECUTIVE') or hasRole('ROLE_SUPPORT_ADMIN') or hasRole('ROLE_SUPPORT_MANAGER')")
    public ResponseEntity<KBArticle> createArticle(@Valid @RequestBody CreateKBArticleRequest request, Authentication auth) {
        KBArticle article = new KBArticle(
            request.getTitle(),
            request.getContent(),
            request.getTags(),
            auth.getName()
        );
        article.setCategory(request.getCategory());
        if (request.getVisibility() != null) {
            article.setVisibility(request.getVisibility());
        }
        article.setPublished(Boolean.TRUE.equals(request.getPublished()));
        KBArticle saved = kbArticleService.createArticle(article);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPPORT_EXECUTIVE') or hasRole('ROLE_SUPPORT_ADMIN') or hasRole('ROLE_SUPPORT_MANAGER')")
    public ResponseEntity<KBArticle> updateArticle(@PathVariable Long id, @Valid @RequestBody CreateKBArticleRequest request, Authentication auth) {
        KBArticle updatedArticle = new KBArticle();
        updatedArticle.setTitle(request.getTitle());
        updatedArticle.setContent(request.getContent());
        updatedArticle.setTags(request.getTags());
        updatedArticle.setUpdatedBy(auth.getName());
        updatedArticle.setCategory(request.getCategory());
        updatedArticle.setVisibility(request.getVisibility());
        updatedArticle.setPublished(Boolean.TRUE.equals(request.getPublished()));

        KBArticle result = kbArticleService.updateArticle(id, updatedArticle);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_SUPPORT_EXECUTIVE') or hasRole('ROLE_SUPPORT_ADMIN') or hasRole('ROLE_SUPPORT_MANAGER')")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        boolean deleted = kbArticleService.deleteArticle(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}