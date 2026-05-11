package com.primecx.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recordings")
public class Recording {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private SupportSession session;

    @Column(nullable = false)
    private String s3Key;

    @Column(nullable = false)
    private String s3Bucket;

    private String fileName;

    private Long fileSize;

    private Integer durationSeconds;

    private String contentType;

    private LocalDateTime uploadedAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean legalHold = false;

    private LocalDateTime deletedAt;

    @PrePersist
    protected void onUpload() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}
