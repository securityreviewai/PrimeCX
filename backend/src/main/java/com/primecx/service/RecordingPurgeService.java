package com.primecx.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.model.Organization;
import com.primecx.model.Recording;
import com.primecx.model.User;
import com.primecx.repository.RecordingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingPurgeService {

    private static final int PAGE = 200;

    private final RecordingRepository recordingRepository;
    private final RetentionPolicyService retentionPolicyService;
    private final S3StorageService s3StorageService;

    public static boolean isEligibleForPurge(Recording r, RetentionPolicyService.ResolvedRetention pol,
            LocalDateTime now) {
        if (r.isLegalHold()) {
            return false;
        }
        if (r.getDeletedAt() == null) {
            return !r.getUploadedAt().plusDays(pol.retentionDays()).isAfter(now);
        }
        return !r.getDeletedAt().plusDays(pol.softDeleteGraceDays()).isAfter(now);
    }

    /**
     * Scans all non-hold recordings in pages, physically deletes S3 and DB when policy allows.
     */
    @Transactional
    public int runPurgePass() {
        LocalDateTime now = LocalDateTime.now();
        int purged = 0;
        long lastId = 0L;
        while (true) {
            Slice<Recording> batch = recordingRepository
                    .findByIdGreaterThanAndLegalHoldIsFalseOrderByIdAsc(lastId, PageRequest.of(0, PAGE));
            if (!batch.hasContent()) {
                break;
            }
            for (Recording r : batch) {
                lastId = r.getId();
                if (r.getSession() == null) {
                    log.warn("Recording {} has no session; skipping purge", r.getId());
                    continue;
                }
                User executive = r.getSession().getSupportExecutive();
                Organization org = executive != null ? executive.getOrganization() : null;
                var pol = retentionPolicyService.resolve(org, r.getS3Bucket());
                if (isEligibleForPurge(r, pol, now)) {
                    s3StorageService.deleteObject(r.getS3Key());
                    recordingRepository.delete(r);
                    purged++;
                }
            }
            if (!batch.hasNext()) {
                break;
            }
        }
        if (purged > 0) {
            log.info("Recording purge pass removed {} object(s) from S3 and database", purged);
        }
        return purged;
    }
}
