package com.primecx.service;

import com.primecx.model.KBArticle;
import com.primecx.model.KBVisibility;
import com.primecx.repository.KBArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KBArticleService {

    @Autowired
    private KBArticleRepository kbArticleRepository;

    public List<KBArticle> searchArticles(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        List<KBArticle> results = kbArticleRepository.searchByQuery(query.trim());
        return results.size() > 20 ? results.subList(0, 20) : results;
    }

    public KBArticle getArticleById(Long id) {
        return kbArticleRepository.findById(id).orElse(null);
    }

    public List<KBArticle> getAllArticles() {
        return kbArticleRepository.findAll();
    }

    @Transactional
    public KBArticle createArticle(KBArticle article) {
        article.setCreatedAt(LocalDateTime.now());
        if (article.getVisibility() == null) {
            article.setVisibility(KBVisibility.INTERNAL_ONLY);
        }
        return kbArticleRepository.save(article);
    }

    @Transactional
    public KBArticle updateArticle(Long id, KBArticle updatedArticle) {
        KBArticle existing = kbArticleRepository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }
        existing.setTitle(updatedArticle.getTitle());
        existing.setContent(updatedArticle.getContent());
        existing.setTags(updatedArticle.getTags());
        existing.setUpdatedBy(updatedArticle.getUpdatedBy());
        if (updatedArticle.getCategory() != null) {
            existing.setCategory(updatedArticle.getCategory());
        }
        if (updatedArticle.getVisibility() != null) {
            existing.setVisibility(updatedArticle.getVisibility());
        }
        existing.setPublished(updatedArticle.isPublished());
        existing.setUpdatedAt(LocalDateTime.now());
        return kbArticleRepository.save(existing);
    }

    @Transactional
    public boolean deleteArticle(Long id) {
        if (kbArticleRepository.existsById(id)) {
            kbArticleRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
