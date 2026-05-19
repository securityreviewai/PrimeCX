package com.primecx.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.RecordingDto;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Recording;
import com.primecx.model.SupportSession;
import com.primecx.model.User;
import com.primecx.repository.RecordingRepository;
import com.primecx.repository.SupportSessionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingService {

    private final RecordingRepository recordingRepository;
    private final SupportSessionRepository supportSessionRepository;
    private final S3StorageService s3StorageService;
    private final TicketService ticketService;

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

        log.info("Saving recording metadata for session {} - file: {}", sessionId, fileName);
        return recordingRepository.save(recording);
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

    /**
     * All session recordings for a ticket, newest upload first. Caller must be allowed to view the ticket.
     */
    @Transactional(readOnly = true)
    public List<RecordingDto> listRecordingsForTicket(Long ticketId, User viewer) {
        ticketService.getTicketVisibleTo(ticketId, viewer);
        return recordingRepository.findBySession_Ticket_IdOrderByUploadedAtDesc(ticketId).stream()
                .map(r -> toDto(r, false))
                .toList();
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
                presignedUrl
        );
    }
}
