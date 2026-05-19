package com.primecx.repository;

import com.primecx.model.RecordingQaReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecordingQaReviewRepository extends JpaRepository<RecordingQaReview, Long> {

    List<RecordingQaReview> findByRecordingIdOrderByCreatedAtDesc(Long recordingId);

    Optional<RecordingQaReview> findTopByRecordingIdAndReviewerIdOrderByCreatedAtDesc(Long recordingId, Long reviewerId);

    List<RecordingQaReview> findTop20ByOrderByCreatedAtDesc();
}
