package com.primecx.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.primecx.service.RecordingPurgeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Schedules best-effort physical deletion of S3 objects and DB rows for recordings that are
 * not on legal hold and that satisfy the resolved retention policy. Disable with
 * {@code primecx.storage.purge-enabled=false} if the job should not run in an environment
 * (e.g. local dev with no S3).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "primecx.storage.purge-enabled", havingValue = "true", matchIfMissing = true)
public class RecordingPurgeScheduler {

    private final RecordingPurgeService recordingPurgeService;

    @Scheduled(cron = "${primecx.storage.purge-cron:0 0 3 * * *}")
    public void runScheduledPurge() {
        try {
            int n = recordingPurgeService.runPurgePass();
            if (n > 0) {
                log.info("Scheduled recording purge completed; {} removed", n);
            }
        } catch (Exception e) {
            log.error("Scheduled recording purge failed", e);
        }
    }
}
