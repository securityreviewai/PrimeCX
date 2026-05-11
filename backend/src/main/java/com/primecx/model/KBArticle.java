package com.primecx.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "kb_articles")
public class KBArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 1000)
    private String tags; // comma-separated

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private String updatedBy;

    @Column
    private LocalDateTime updatedAt;

    /**
     * Authoring-side category used to group customer-facing KB articles on the portal.
     * Nullable to stay compatible with legacy rows; portal queries treat null as non-browseable.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private KBArticleCategory category;

    /**
     * Controls whether the article may appear in the customer-facing KB portal.
     * Legacy rows default to INTERNAL_ONLY (safest) through {@link #prePersistDefaults()}.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private KBVisibility visibility;

    /**
     * Only published + PUBLIC articles are served to customer-portal readers.
     * Defaults to {@code false} so drafts are never leaked to the portal.
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean published;

    public KBArticle() {}

    public KBArticle(String title, String content, String tags, String createdBy) {
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersistDefaults() {
        if (visibility == null) {
            visibility = KBVisibility.INTERNAL_ONLY;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public KBArticleCategory getCategory() { return category; }
    public void setCategory(KBArticleCategory category) { this.category = category; }

    public KBVisibility getVisibility() { return visibility; }
    public void setVisibility(KBVisibility visibility) { this.visibility = visibility; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
}
