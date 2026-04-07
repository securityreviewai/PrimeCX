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
@Table(name = "transcript_analyses")
public class TranscriptAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SupportSession session;

    @Enumerated(EnumType.STRING)
    private SentimentType sentimentType;

    private Double sentimentScore;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String keyTopics;

    private Double customerSatisfactionScore;

    private String resolutionQuality;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String suggestedFollowUps;

    private String escalationRisk;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String rawAiResponse;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AnalysisStatus status = AnalysisStatus.PENDING;

    private String errorMessage;

    private LocalDateTime analyzedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
