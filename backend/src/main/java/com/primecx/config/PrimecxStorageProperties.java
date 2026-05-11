package com.primecx.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "primecx.storage")
public class PrimecxStorageProperties {

    /**
     * When no {@link com.primecx.model.RetentionPolicy} row applies, max age in days before eligible purge.
     */
    private int defaultRetentionDays = 2555;

    /**
     * Default days after soft-delete before physical purge when no policy row applies.
     */
    private int defaultSoftDeleteGraceDays = 30;

    /**
     * Spring cron expression for the scheduled purge job (S3 delete + DB row remove for eligible recordings).
     */
    private String purgeCron = "0 0 3 * * *";

    /**
     * When true, prefer application-level purge and legal-hold semantics; keep S3 lifecycle expiration
     * disabled or set to at least the effective retention so objects are not removed out-of-band.
     */
    private boolean appIsSourceOfTruthForDeletion = true;

    /**
     * When false, the scheduled S3+DB purge job is not registered.
     */
    private boolean purgeEnabled = true;
}
