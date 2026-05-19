package com.primecx.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CreateQaReviewRequest;
import com.primecx.dto.RecordingQaReviewDto;
import com.primecx.exception.ForbiddenException;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Recording;
import com.primecx.model.RecordingQaReview;
import com.primecx.model.Role;
import com.primecx.model.User;
import com.primecx.repository.RecordingQaReviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingQaService {

    private final RecordingQaReviewRepository qaReviewRepository;
    private final RecordingService recordingService;

    @Transactional
    public RecordingQaReviewDto createReview(Long recordingId, CreateQaReviewRequest request, User reviewer) {
        requireManagerOrAdmin(reviewer);
        validateScores(request);

        Recording recording = recordingService.getRecordingById(recordingId);

        RecordingQaReview review = RecordingQaReview.builder()
                .recording(recording)
                .reviewer(reviewer)
                .empathyScore(request.empathyScore())
                .accuracyScore(request.accuracyScore())
                .complianceScore(request.complianceScore())
                .notes(request.notes())
                .build();

        review = qaReviewRepository.save(review);
        log.info("QA review {} created for recording {} by user {}", review.getId(), recordingId, reviewer.getId());
        return toDto(review);
    }

    public List<RecordingQaReviewDto> getReviewsForRecording(Long recordingId) {
        return qaReviewRepository.findByRecordingIdOrderByCreatedAtDesc(recordingId).stream()
                .map(this::toDto)
                .toList();
    }

    public List<RecordingQaReviewDto> getRecentReviews() {
        return qaReviewRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    private void validateScores(CreateQaReviewRequest request) {
        validateScore(request.empathyScore(), "empathy");
        validateScore(request.accuracyScore(), "accuracy");
        validateScore(request.complianceScore(), "compliance");
    }

    private void validateScore(Integer score, String field) {
        if (score == null || score < 1 || score > 5) {
            throw new IllegalArgumentException(field + " score must be between 1 and 5");
        }
    }

    private void requireManagerOrAdmin(User user) {
        if (user.getRole() != Role.ROLE_SUPPORT_ADMIN && user.getRole() != Role.ROLE_SUPPORT_MANAGER) {
            throw new ForbiddenException("Only managers and admins can submit QA reviews");
        }
    }

    public RecordingQaReviewDto toDto(RecordingQaReview review) {
        User reviewer = review.getReviewer();
        String reviewerName = reviewer.getFirstName() + " " + reviewer.getLastName();
        int overall = Math.round(
                (review.getEmpathyScore() + review.getAccuracyScore() + review.getComplianceScore()) / 3.0f);

        return new RecordingQaReviewDto(
                review.getId(),
                review.getRecording().getId(),
                reviewer.getId(),
                reviewerName,
                review.getEmpathyScore(),
                review.getAccuracyScore(),
                review.getComplianceScore(),
                overall,
                review.getNotes(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
