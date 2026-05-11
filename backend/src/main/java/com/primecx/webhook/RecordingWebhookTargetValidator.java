package com.primecx.webhook;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import org.springframework.stereotype.Component;

import com.primecx.config.RecordingWebhookProperties;

import lombok.RequiredArgsConstructor;

/**
 * Validates configured webhook targets to reduce SSRF risk from misconfiguration (egress guardrail).
 */
@Component
@RequiredArgsConstructor
public class RecordingWebhookTargetValidator {

    private final RecordingWebhookProperties properties;

    public void validate(URI uri) throws UnknownHostException {
        String scheme = uri.getScheme();
        if (scheme == null || (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("Recording webhook URL must use http or https");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Recording webhook URL must include a host");
        }

        boolean allowNonPublic = properties.isAllowNonPublicTargets();
        for (InetAddress addr : InetAddress.getAllByName(host)) {
            if (isDisallowedAddress(addr, allowNonPublic)) {
                throw new IllegalArgumentException(
                        "Recording webhook host resolves to a disallowed address; "
                                + "set primecx.webhooks.recording.allow-non-public-targets=true only for local testing");
            }
        }
    }

    private static boolean isDisallowedAddress(InetAddress addr, boolean allowNonPublic) {
        if (allowNonPublic) {
            return false;
        }
        if (addr.isMulticastAddress()) {
            return true;
        }
        if (addr.isLinkLocalAddress()) {
            return true;
        }
        if (addr.isLoopbackAddress()) {
            return true;
        }
        if (addr instanceof Inet4Address v4) {
            byte[] b = v4.getAddress();
            int b0 = b[0] & 0xff;
            int b1 = b[1] & 0xff;
            if (b0 == 10) {
                return true;
            }
            if (b0 == 172 && b1 >= 16 && b1 <= 31) {
                return true;
            }
            if (b0 == 192 && b1 == 168) {
                return true;
            }
        }
        return false;
    }
}
