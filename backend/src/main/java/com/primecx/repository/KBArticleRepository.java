package com.primecx.repository;

import com.primecx.model.KBArticle;
import com.primecx.model.KBArticleCategory;
import com.primecx.model.KBVisibility;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KBArticleRepository extends JpaRepository<KBArticle, Long> {

    @Query("SELECT k FROM KBArticle k WHERE " +
           "LOWER(k.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(k.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(k.tags) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY k.updatedAt DESC")
    List<KBArticle> searchByQuery(@Param("query") String query);

    /**
     * Customer-portal search: scoped to published + PUBLIC articles only. Uses named parameters to
     * prevent JPQL injection and is invoked with a pre-escaped LIKE pattern to prevent
     * wildcard-abuse DoS.
     */
    @Query("SELECT k FROM KBArticle k WHERE " +
           "k.published = TRUE AND k.visibility = :visibility AND (" +
           " LOWER(k.title) LIKE :pattern ESCAPE '\\' OR " +
           " LOWER(k.content) LIKE :pattern ESCAPE '\\' OR " +
           " LOWER(k.tags) LIKE :pattern ESCAPE '\\' ) " +
           "ORDER BY k.updatedAt DESC")
    List<KBArticle> searchPublic(
            @Param("pattern") String pattern,
            @Param("visibility") KBVisibility visibility,
            Pageable pageable);

    /**
     * BOLA-safe fetch: returns only if the article is both published and PUBLIC. An INTERNAL_ONLY
     * or unpublished id never resolves here, so portal callers cannot retrieve it by guessing ids.
     */
    Optional<KBArticle> findByIdAndPublishedTrueAndVisibility(Long id, KBVisibility visibility);

    List<KBArticle> findByPublishedTrueAndVisibilityOrderByUpdatedAtDesc(
            KBVisibility visibility, Pageable pageable);

    List<KBArticle> findByPublishedTrueAndVisibilityAndCategoryOrderByUpdatedAtDesc(
            KBVisibility visibility, KBArticleCategory category, Pageable pageable);

    long countByPublishedTrueAndVisibility(KBVisibility visibility);

    long countByPublishedTrueAndVisibilityAndCategory(
            KBVisibility visibility, KBArticleCategory category);

    /**
     * Related articles: same category, PUBLIC + published, excluding the current id. Used by the
     * customer portal's "related articles" section.
     */
    List<KBArticle> findByPublishedTrueAndVisibilityAndCategoryAndIdNotOrderByUpdatedAtDesc(
            KBVisibility visibility, KBArticleCategory category, Long excludeId, Pageable pageable);
}
