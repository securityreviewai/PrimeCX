package com.primecx.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.primecx.config.RecordingRetentionConfig;
import com.primecx.dto.CompletedPartDto;
import com.primecx.dto.MultipartCompleteRequest;
import com.primecx.dto.MultipartInitRequest;
import com.primecx.dto.MultipartInitResponse;
import com.primecx.dto.MultipartPartUrlRequest;
import com.primecx.dto.MultipartPartUrlResponse;
import com.primecx.dto.RecordingAccessAuditDto;
import com.primecx.dto.RecordingDto;
import com.primecx.dto.RecordingPlaybackDto;
import com.primecx.dto.RedactionRegionDto;
import com.primecx.dto.RetentionPolicyDto;
import com.primecx.dto.SaveRedactionsRequest;
import com.primecx.exception.ForbiddenException;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Recording;
import com.primecx.model.RecordingAccessAudit;
import com.primecx.model.RecordingAccessType;
import com.primecx.model.RecordingUpload;
import com.primecx.model.Role;
import com.primecx.model.SupportSession;
import com.primecx.model.User;
import com.primecx.repository.RecordingAccessAuditRepository;
import com.primecx.repository.RecordingRepository;
import com.primecx.repository.RecordingUploadRepository;
import com.primecx.repository.SupportSessionRepository;
import com.primecx.service.S3StorageService.MultipartUploadInitResult;
import com.primecx.service.S3StorageService.PartEtag;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RecordingService {

    private final RecordingRepository recordingRepository;
    private final SupportSessionRepository supportSessionRepository;
    private final RecordingUploadRepository recordingUploadRepository;
    private final RecordingAccessAuditRepository accessAuditRepository;
    private final S3StorageService s3StorageService;
    private final RecordingRetentionConfig retentionConfig;
    private final RecordingQaService recordingQaService;
    private final PostSessionAnalysisService postSessionAnalysisService;
    private final ObjectMapper objectMapper;

    public RecordingService(
            RecordingRepository recordingRepository,
            SupportSessionRepository supportSessionRepository,
            RecordingUploadRepository recordingUploadRepository,
            RecordingAccessAuditRepository accessAuditRepository,
            S3StorageService s3StorageService,
            RecordingRetentionConfig retentionConfig,
            @Lazy RecordingQaService recordingQaService,
            @Lazy PostSessionAnalysisService postSessionAnalysisService,
            ObjectMapper objectMapper) {
        this.recordingRepository = recordingRepository;
        this.supportSessionRepository = supportSessionRepository;
        this.recordingUploadRepository = recordingUploadRepository;
        this.accessAuditRepository = accessAuditRepository;
        this.s3StorageService = s3StorageService;
        this.retentionConfig = retentionConfig;
        this.recordingQaService = recordingQaService;
        this.postSessionAnalysisService = postSessionAnalysisService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MultipartInitResponse initiateMultipartUpload(MultipartInitRequest request) {
        SupportSession session = supportSessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("SupportSession", request.sessionId()));

        String fileName = request.fileName() != null ? request.fileName() : "recording.webm";
        String contentType = request.contentType() != null ? request.contentType() : "video/webm";
        String s3Key = s3StorageService.generateRecordingKey(request.sessionId(), fileName);

        MultipartUploadInitResult init = s3StorageService.initiateMultipartUpload(s3Key, contentType);

        RecordingUpload upload = RecordingUpload.builder()
                .session(session)
                .s3Key(init.s3Key())
                .uploadId(init.uploadId())
                .fileName(fileName)
                .contentType(contentType)
                .status("IN_PROGRESS")
                .build();
        upload = recordingUploadRepository.save(upload);

        return new MultipartInitResponse(init.uploadId(), init.s3Key(), upload.getId());
    }

    public MultipartPartUrlResponse getMultipartPartUrl(MultipartPartUrlRequest request) {
        recordingUploadRepository.findByS3KeyAndUploadId(request.s3Key(), request.uploadId())
                .orElseThrow(() -> new ResourceNotFoundException("RecordingUpload", request.uploadId()));

        String url = s3StorageService.generatePresignedUploadPartUrl(
                request.s3Key(), request.uploadId(), request.partNumber());
        return new MultipartPartUrlResponse(url, request.partNumber());
    }

    @Transactional
    public RecordingDto completeMultipartUpload(MultipartCompleteRequest request) {
        RecordingUpload upload = recordingUploadRepository.findByS3KeyAndUploadId(request.s3Key(), request.uploadId())
                .orElseThrow(() -> new ResourceNotFoundException("RecordingUpload", request.uploadId()));

        List<PartEtag> parts = request.parts().stream()
                .map(p -> new PartEtag(p.partNumber(), p.etag()))
                .toList();
        s3StorageService.completeMultipartUpload(request.s3Key(), request.uploadId(), parts);

        upload.setStatus("COMPLETED");
        recordingUploadRepository.save(upload);

        Recording recording = saveRecordingMetadata(
                request.sessionId(),
                request.fileName(),
                request.contentType(),
                request.fileSize(),
                request.duration(),
                request.s3Key());

        postSessionAnalysisService.transcribeAndAnalyzeRecording(recording.getId());
        return toDto(recording);
    }

    public void triggerPostUploadAnalysis(Long recordingId) {
        postSessionAnalysisService.transcribeAndAnalyzeRecording(recordingId);
    }

    @Transactional
    public Recording saveRecordingMetadata(Long sessionId, String fileName, String contentType,
                                           Long fileSize, Integer duration, String s3Key) {
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
        recording.setRetentionExpiresAt(LocalDateTime.now().plusDays(retentionConfig.getRetentionDays()));

        log.info("Saving recording metadata for session {} - file: {}", sessionId, fileName);
        return recordingRepository.save(recording);
    }

    @Transactional
    public void updateTranscript(Long recordingId, String transcript) {
        Recording recording = getRecordingById(recordingId);
        recording.setTranscript(transcript);
        recordingRepository.save(recording);
    }

    public List<Recording> getAllRecordings() {
        return recordingRepository.findAll();
    }

    public List<Recording> getRecordingsBySession(Long sessionId) {
        return recordingRepository.findBySessionId(sessionId);
    }

    public Recording getRecordingById(Long id) {
        return recordingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recording", id));
    }

    public List<Recording> getRecordingsByExecutive(Long executiveId) {
        return recordingRepository.findBySession_SupportExecutiveId(executiveId);
    }

    @Transactional
    public RecordingPlaybackDto getPlayback(Long recordingId, User viewer, String ipAddress) {
        requireManagerOrAdmin(viewer);
        Recording recording = getRecordingById(recordingId);
        checkRetention(recording);

        logAccess(recording, viewer, RecordingAccessType.PLAYBACK, ipAddress);

        return new RecordingPlaybackDto(
                recording.getId(),
                recording.getSession().getId(),
                recording.getFileName(),
                recording.getFileSize(),
                recording.getDurationSeconds(),
                recording.getContentType(),
                recording.getUploadedAt(),
                s3StorageService.generatePresignedDownloadUrl(recording.getS3Key()),
                recording.getTranscript(),
                parseRedactions(recording.getRedactionRegions()),
                recording.getRetentionExpiresAt(),
                recordingQaService.getReviewsForRecording(recordingId)
        );
    }

    @Transactional
    public RecordingDto saveRedactions(Long recordingId, SaveRedactionsRequest request, User user) {
        requireManagerOrAdmin(user);
        Recording recording = getRecordingById(recordingId);
        try {
            recording.setRedactionRegions(objectMapper.writeValueAsString(request.regions()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid redaction regions");
        }
        recordingRepository.save(recording);
        logAccess(recording, user, RecordingAccessType.VIEW, null);
        return toDto(recording);
    }

    public List<RecordingAccessAuditDto> getAccessAudit(Long recordingId, User user) {
        requireManagerOrAdmin(user);
        getRecordingById(recordingId);
        return accessAuditRepository.findByRecordingIdOrderByAccessedAtDesc(recordingId).stream()
                .map(this::toAuditDto)
                .toList();
    }

    public RetentionPolicyDto getRetentionPolicy() {
        return new RetentionPolicyDto(
                retentionConfig.getRetentionDays(),
                retentionConfig.getTransitionToIaDays(),
                retentionConfig.getTransitionToGlacierDays(),
                retentionConfig.isAutoTranscriptionEnabled(),
                retentionConfig.isAutoAnalysisOnSessionEnd()
        );
    }

    @Transactional
    public void deleteRecording(Long id) {
        Recording recording = getRecordingById(id);
        s3StorageService.deleteObject(recording.getS3Key());
        recordingRepository.delete(recording);
        log.info("Deleted recording {} and S3 object {}", id, recording.getS3Key());
    }

    public RecordingDto toDto(Recording recording) {
        return toDto(recording, false);
    }

    public RecordingDto toDto(Recording recording, boolean includePresignedUrl) {
        String presignedUrl = null;
        if (includePresignedUrl) {
            presignedUrl = s3StorageService.generatePresignedDownloadUrl(recording.getS3Key());
        }

        return new RecordingDto(
                recording.getId(),
                recording.getSession().getId(),
                recording.getS3Key(),
                recording.getFileName(),
                recording.getFileSize(),
                recording.getDurationSeconds(),
                recording.getContentType(),
                recording.getUploadedAt(),
                presignedUrl,
                recording.getTranscript(),
                recording.getRetentionExpiresAt(),
                recording.getRedactionRegions() != null && !recording.getRedactionRegions().isBlank()
        );
    }

    private void logAccess(Recording recording, User user, RecordingAccessType type, String ipAddress) {
        RecordingAccessAudit audit = RecordingAccessAudit.builder()
                .recording(recording)
                .user(user)
                .accessType(type)
                .ipAddress(ipAddress)
                .build();
        accessAuditRepository.save(audit);
    }

    private void checkRetention(Recording recording) {
        if (recording.getRetentionExpiresAt() != null
                && recording.getRetentionExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ForbiddenException("Recording has expired per retention policy");
        }
    }

    private void requireManagerOrAdmin(User user) {
        if (user.getRole() != Role.ROLE_SUPPORT_ADMIN && user.getRole() != Role.ROLE_SUPPORT_MANAGER) {
            throw new ForbiddenException("Only managers and admins can access recording playback");
        }
    }

    private List<RedactionRegionDto> parseRedactions(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<RedactionRegionDto>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse redaction regions: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private RecordingAccessAuditDto toAuditDto(RecordingAccessAudit audit) {
        User user = audit.getUser();
        return new RecordingAccessAuditDto(
                audit.getId(),
                audit.getRecording().getId(),
                user.getId(),
                user.getFirstName() + " " + user.getLastName(),
                audit.getAccessType().name(),
                audit.getIpAddress(),
                audit.getAccessedAt()
        );
    }
}
