package com.primecx.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.config.PrimecxStorageProperties;
import com.primecx.model.Organization;
import com.primecx.model.RetentionPolicy;
import com.primecx.repository.RetentionPolicyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionPolicyService {

    private static final int MIN_DAYS = 1;
    private static final int MAX_RET_DAYS = 36_500;
    private static final int MAX_GRACE = 3_650;

    /**
     * S3 bucket naming (DNS-compliant); relaxed length for test buckets.
     */
    private static final Pattern BUCKET_NAME = Pattern
            .compile("^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$");

    private final RetentionPolicyRepository retentionPolicyRepository;
    private final PrimecxStorageProperties storageProperties;

    public record ResolvedRetention(int retentionDays, int softDeleteGraceDays) {
    }

    @Transactional(readOnly = true)
    public ResolvedRetention resolve(Organization organization, String s3Bucket) {
        if (organization != null) {
            var orgSpecific = retentionPolicyRepository
                    .findByOrganizationIdAndS3Bucket(organization.getId(), s3Bucket);
            if (orgSpecific.isPresent()) {
                return toResolved(orgSpecific.get());
            }
        }
        List<RetentionPolicy> globalForBucket = retentionPolicyRepository
                .findByOrganizationIsNullAndS3Bucket(s3Bucket);
        if (globalForBucket.isEmpty()) {
            return new ResolvedRetention(
                    storageProperties.getDefaultRetentionDays(),
                    storageProperties.getDefaultSoftDeleteGraceDays());
        }
        if (globalForBucket.size() > 1) {
            log.warn("Multiple default retention policy rows for bucket {} — using the first (id={})",
                    s3Bucket, globalForBucket.get(0).getId());
        }
        return toResolved(globalForBucket.get(0));
    }

    public static void validateS3BucketName(String s3Bucket) {
        if (s3Bucket == null || s3Bucket.isBlank() || s3Bucket.length() < 3 || s3Bucket.length() > 255) {
            throw new IllegalArgumentException("Invalid S3 bucket name");
        }
        if (!BUCKET_NAME.matcher(s3Bucket).matches()) {
            throw new IllegalArgumentException("S3 bucket name must be DNS-compatible (lowercase, dots, hyphens).");
        }
    }

    public static void validateDayRanges(int retentionDays, int softDeleteGraceDays) {
        if (retentionDays < MIN_DAYS || retentionDays > MAX_RET_DAYS) {
            throw new IllegalArgumentException("retentionDays must be between " + MIN_DAYS + " and " + MAX_RET_DAYS);
        }
        if (softDeleteGraceDays < MIN_DAYS || softDeleteGraceDays > MAX_GRACE) {
            throw new IllegalArgumentException("softDeleteGraceDays must be between " + MIN_DAYS + " and " + MAX_GRACE);
        }
    }

    @Transactional
    public RetentionPolicy upsert(Organization organization, String s3Bucket, int retentionDays,
            int softDeleteGraceDays) {
        validateS3BucketName(s3Bucket);
        validateDayRanges(retentionDays, softDeleteGraceDays);
        if (organization == null) {
            return upsertDefaultForBucket(s3Bucket, retentionDays, softDeleteGraceDays);
        }
        return retentionPolicyRepository
                .findByOrganizationIdAndS3Bucket(organization.getId(), s3Bucket)
                .map(existing -> {
                    existing.setRetentionDays(retentionDays);
                    existing.setSoftDeleteGraceDays(softDeleteGraceDays);
                    return retentionPolicyRepository.save(existing);
                })
                .orElseGet(() -> retentionPolicyRepository.save(RetentionPolicy.builder()
                        .organization(organization)
                        .s3Bucket(s3Bucket)
                        .retentionDays(retentionDays)
                        .softDeleteGraceDays(softDeleteGraceDays)
                        .build()));
    }

    private RetentionPolicy upsertDefaultForBucket(String s3Bucket, int retentionDays, int softDeleteGraceDays) {
        List<RetentionPolicy> exist = retentionPolicyRepository.findByOrganizationIsNullAndS3Bucket(s3Bucket);
        if (exist.isEmpty()) {
            return retentionPolicyRepository.save(RetentionPolicy.builder()
                    .organization(null)
                    .s3Bucket(s3Bucket)
                    .retentionDays(retentionDays)
                    .softDeleteGraceDays(softDeleteGraceDays)
                    .build());
        }
        RetentionPolicy keep = exist.get(0);
        keep.setRetentionDays(retentionDays);
        keep.setSoftDeleteGraceDays(softDeleteGraceDays);
        retentionPolicyRepository.save(keep);
        for (int i = 1; i < exist.size(); i++) {
            retentionPolicyRepository.delete(exist.get(i));
        }
        return keep;
    }

    @Transactional(readOnly = true)
    public List<RetentionPolicy> listAll() {
        return retentionPolicyRepository.findAll();
    }

    private static ResolvedRetention toResolved(RetentionPolicy p) {
        return new ResolvedRetention(p.getRetentionDays(), p.getSoftDeleteGraceDays());
    }
}
