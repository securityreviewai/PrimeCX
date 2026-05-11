package com.primecx.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.RecordingDto;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Organization;
import com.primecx.model.Recording;
import com.primecx.model.Role;
import com.primecx.model.SupportSession;
import com.primecx.model.User;
import com.primecx.repository.RecordingRepository;
import com.primecx.repository.SupportSessionRepository;
import com.primecx.webhook.RecordingWebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingService {

    private final RecordingRepository recordingRepository;
    private final SupportSessionRepository supportSessionRepository;
    private final S3StorageService s3StorageService;
    private final RecordingWebhookService recordingWebhookService;
    private final AuditLogService auditLogService;
    private final RetentionPolicyService retentionPolicyService;

    @Transactional
    public Recording saveRecordingMetadata(Long sessionId, String fileName, String contentType,
                                           Long fileSize, Integer duration, String s3Key, Long actorUserId) {
        SupportSession session = supportSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportSession", sessionId));

        Recording recording = new Recording();
        recording.setSession(session);
        recording.setS3Key(s3Key);
        recording.setS3Bucket(s3StorageService.getBucketName());
        recording.setFileName(fileName);
        recording.setFileSize(fileSize);
        recording.setDurationSeconds(duration);
        recording.setContentType(contentType);
        recording.setUploadedAt(LocalDateTime.now());
        recording.setLegalHold(false);
        recording.setDeletedAt(null);

        log.info("Saving recording metadata for session {} - file: {}", sessionId, fileName);
        Recording saved = recordingRepository.save(recording);
        auditLogService.appendRecordingConfirmed(actorUserId, saved.getId(), sessionId, fileName);
        recordingWebhookService.notifyRecordingReady(
                saved.getId(),
                sessionId,
                saved.getS3Key(),
                saved.getFileName(),
                saved.getFileSize(),
                saved.getDurationSeconds(),
                saved.getContentType(),
                saved.getUploadedAt());
        return saved;
    }

    public List<Recording> getAllRecordings() {
        return recordingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<RecordingDto> listAllRecordingAdminDtos() {
        return getAllRecordings().stream().map(this::toAdminDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RecordingDto> listRecordingAdminDtosByExecutive(Long executiveId) {
        return getRecordingsByExecutive(executiveId).stream().map(this::toAdminDto).toList();
    }

    public List<Recording> getRecordingsBySession(Long sessionId) {
        return recordingRepository.findBySessionId(sessionId);
    }

    public List<Recording> getRecordingsBySessionForUser(Long sessionId, User user) {
        if (user.getRole() == Role.ROLE_SUPPORT_ADMIN || user.getRole() == Role.ROLE_SUPPORT_MANAGER) {
            return recordingRepository.findBySessionId(sessionId);
        }
        return recordingRepository.findBySessionIdAndDeletedAtIsNull(sessionId);
    }

    public Recording getRecordingById(Long id) {
        return recordingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recording", id));
    }

    public Recording getRecordingByIdForUser(Long id, User user) {
        Recording recording = getRecordingById(id);
        if (recording.getDeletedAt() != null
                && user.getRole() != Role.ROLE_SUPPORT_ADMIN
                && user.getRole() != Role.ROLE_SUPPORT_MANAGER) {
            throw new ResourceNotFoundException("Recording", id);
        }
        return recording;
    }

    /**
     * Issues a time-limited GET URL for the recording object. Authorization is scoped by role:
     * admins/managers may download any recording; executives only recordings for their own sessions.
     * Soft-deleted recordings are not downloadable for non-admin/manager roles.
     */
    public Map<String, Object> issuePresignedDownload(Long recordingId, User user) {
        Role role = user.getRole();
        Recording recording;

        if (role == Role.ROLE_SUPPORT_ADMIN || role == Role.ROLE_SUPPORT_MANAGER) {
            recording = recordingRepository.findById(recordingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Recording", recordingId));
        } else if (role == Role.ROLE_SUPPORT_EXECUTIVE) {
            recording = recordingRepository.findByIdAndSession_SupportExecutive_Id(recordingId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Recording", recordingId));
        } else {
            throw new AccessDeniedException("Recording downloads are not available for this account");
        }

        if (recording.getDeletedAt() != null && role == Role.ROLE_SUPPORT_EXECUTIVE) {
            throw new ResourceNotFoundException("Recording", recordingId);
        }

        String downloadUrl = s3StorageService.generatePresignedDownloadUrl(recording.getS3Key());
        log.info("Issued presigned recording download recordingId={} userId={} role={}",
                recordingId, user.getId(), role);

        return Map.of(
                "downloadUrl", downloadUrl,
                "expiresInSeconds", s3StorageService.presignedDownloadUrlTtlSeconds());
    }

    public List<Recording> getRecordingsByExecutive(Long executiveId) {
        return recordingRepository.findBySession_SupportExecutiveId(executiveId);
    }

    @Transactional
    public void softDeleteRecording(Long id, Long actorUserId) {
        Recording recording = getRecordingById(id);
        if (recording.getDeletedAt() != null) {
            return;
        }
        recording.setDeletedAt(LocalDateTime.now());
        recordingRepository.save(recording);
        auditLogService.appendRecordingSoftDeleted(actorUserId, recording.getId(), recording.getSession().getId(),
                recording.getS3Key());
    }

    @Transactional
    public Recording setLegalHold(Long id, boolean legalHold, Long actorUserId) {
        Recording recording = getRecordingById(id);
        if (recording.isLegalHold() == legalHold) {
            return recording;
        }
        recording.setLegalHold(legalHold);
        Recording saved = recordingRepository.save(recording);
        auditLogService.appendRecordingLegalHoldChanged(
                actorUserId, saved.getId(), saved.getSession().getId(), legalHold);
        return saved;
    }

    public RecordingDto toDto(Recording recording) {
        return toDto(recording, false);
    }

    public RecordingDto toDto(Recording recording, boolean includePresignedUrl) {
        String presignedUrl = null;
        if (includePresignedUrl) {
            presignedUrl = s3StorageService.generatePresignedDownloadUrl(recording.getS3Key());
        }
        return RecordingDto.basic(
                recording.getId(),
                recording.getSession().getId(),
                recording.getS3Key(),
                recording.getFileName(),
                recording.getFileSize(),
                recording.getDurationSeconds(),
                recording.getContentType(),
                recording.getUploadedAt(),
                presignedUrl);
    }

    public RecordingDto toAdminDto(Recording recording) {
        Organization org = null;
        if (recording.getSession() != null && recording.getSession().getSupportExecutive() != null) {
            org = recording.getSession().getSupportExecutive().getOrganization();
        }
        var pol = retentionPolicyService.resolve(org, recording.getS3Bucket());
        String summary = "retention " + pol.retentionDays() + "d, soft-delete grace " + pol.softDeleteGraceDays() + "d";
        return new RecordingDto(
                recording.getId(),
                recording.getSession().getId(),
                recording.getS3Key(),
                recording.getFileName(),
                recording.getFileSize(),
                recording.getDurationSeconds(),
                recording.getContentType(),
                recording.getUploadedAt(),
                null,
                recording.isLegalHold(),
                recording.getDeletedAt(),
                recording.getS3Bucket(),
                summary);
    }
}
