package com.primecx.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-bucket and optional per-(organization,bucket) retention. When {@code organization} is null, the
 * row is the default policy for that S3 bucket (one logical default per bucket; enforced in
 * {@link com.primecx.service.RetentionPolicyService}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "retention_policies")
public class RetentionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "s3_bucket", nullable = false, length = 255)
    private String s3Bucket;

    /**
     * Maximum age in days; active (non–soft-deleted) recordings are purged after this many days from
     * {@code uploadedAt} when not on legal hold.
     */
    @Column(nullable = false)
    private int retentionDays;

    /**
     * After soft delete, the object is eligible for physical purge this many days after
     * {@code Recording.deletedAt}.
     */
    @Column(nullable = false)
    private int softDeleteGraceDays;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        var now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
