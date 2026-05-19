package com.primecx.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.model.Recording;
import com.primecx.repository.RecordingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingRetentionService {

    private final RecordingRepository recordingRepository;
    private final S3StorageService s3StorageService;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredRecordings() {
        List<Recording> expired = recordingRepository.findByRetentionExpiresAtBefore(LocalDateTime.now());
        for (Recording recording : expired) {
            try {
                s3StorageService.deleteObject(recording.getS3Key());
                recordingRepository.delete(recording);
                log.info("Purged expired recording {} (key {})", recording.getId(), recording.getS3Key());
            } catch (Exception e) {
                log.error("Failed to purge recording {}: {}", recording.getId(), e.getMessage());
            }
        }
    }
}
