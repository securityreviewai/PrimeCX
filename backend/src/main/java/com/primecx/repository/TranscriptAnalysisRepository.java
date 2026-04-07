package com.primecx.repository;

import com.primecx.model.AnalysisStatus;
import com.primecx.model.SentimentType;
import com.primecx.model.TranscriptAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranscriptAnalysisRepository extends JpaRepository<TranscriptAnalysis, Long> {

    Optional<TranscriptAnalysis> findBySessionId(Long sessionId);

    List<TranscriptAnalysis> findBySession_SupportExecutiveId(Long executiveId);

    List<TranscriptAnalysis> findByStatus(AnalysisStatus status);

    List<TranscriptAnalysis> findBySentimentType(SentimentType sentimentType);

    long countBySentimentType(SentimentType sentimentType);

    @Query("SELECT AVG(ta.sentimentScore) FROM TranscriptAnalysis ta WHERE ta.status = 'COMPLETED'")
    Optional<Double> findAverageSentimentScore();

    @Query("SELECT AVG(ta.customerSatisfactionScore) FROM TranscriptAnalysis ta WHERE ta.status = 'COMPLETED'")
    Optional<Double> findAverageCustomerSatisfactionScore();

    List<TranscriptAnalysis> findTop10ByOrderByCreatedAtDesc();
}
