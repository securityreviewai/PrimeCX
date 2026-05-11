package com.primecx.dto;

import com.primecx.model.KBArticleCategory;
import com.primecx.model.KBVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateKBArticleRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must be less than 500 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 50000, message = "Content must be less than 50000 characters")
    private String content;

    @Size(max = 1000, message = "Tags must be less than 1000 characters")
    private String tags;

    /**
     * Optional. When provided, must be a known {@link KBArticleCategory}. Jackson enforces enum
     * allowlist on deserialization; callers passing unknown values will receive 400.
     */
    private KBArticleCategory category;

    /**
     * Optional. When null, the service defaults to INTERNAL_ONLY so drafts never leak to the
     * customer-facing portal by accident.
     */
    private KBVisibility visibility;

    /**
     * Optional. Defaults to {@code false} in the service so articles require an explicit publish
     * step before being served to customer-portal readers.
     */
    private Boolean published;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public KBArticleCategory getCategory() { return category; }
    public void setCategory(KBArticleCategory category) { this.category = category; }

    public KBVisibility getVisibility() { return visibility; }
    public void setVisibility(KBVisibility visibility) { this.visibility = visibility; }

    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }
}
