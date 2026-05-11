package com.primecx.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Optional outbound webhook when a recording upload is confirmed.
 * Set {@code callback-url} and {@code signing-secret} to enable delivery.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "primecx.webhooks.recording")
public class RecordingWebhookProperties {

    /**
     * HTTPS (or HTTP for local dev) URL to POST {@code recording.ready} events to.
     */
    private String callbackUrl = "";

    /**
     * Shared secret for HMAC-SHA256 signatures (hex digest prefixed with {@code v1=}).
     */
    private String signingSecret = "";

    /**
     * When false, blocks loopback, RFC1918, and link-local resolved addresses (including cloud metadata ranges).
     * Enable only for local integration testing.
     */
    private boolean allowNonPublicTargets = false;

    public boolean isConfigured() {
        return callbackUrl != null && !callbackUrl.isBlank()
                && signingSecret != null && !signingSecret.isBlank();
    }

    public URI requireCallbackUri() {
        return URI.create(callbackUrl.strip());
    }
}
