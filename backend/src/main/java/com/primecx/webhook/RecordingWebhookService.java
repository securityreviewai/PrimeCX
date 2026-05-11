package com.primecx.webhook;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.primecx.config.RecordingWebhookProperties;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * Fires an optional signed JSON POST when recording metadata is persisted after upload confirmation.
 * Payload includes a stable {@code idempotencyKey} ({@code recording:{id}}) for integration deduplication.
 *
 * <p><strong>Verifier (integrators):</strong> Read raw body bytes as UTF-8. Let {@code t} be the integer
 * seconds from {@code X-PrimeCX-Webhook-Timestamp}. Recompute {@code HMAC-SHA256(secret, t + "." + rawBody)}
 * and compare to the hex digest in {@code X-PrimeCX-Webhook-Signature} after the {@code v1=} prefix using
 * a constant-time equality check. Reject stale timestamps (e.g. skew &gt; 5 minutes) to limit replay.
 */
@Slf4j
@Service
public class RecordingWebhookService {

    public static final String EVENT_RECORDING_READY = "recording.ready";

    private static final String HEADER_TIMESTAMP = "X-PrimeCX-Webhook-Timestamp";
    private static final String HEADER_SIGNATURE = "X-PrimeCX-Webhook-Signature";
    private static final String HEADER_IDEMPOTENCY = "X-PrimeCX-Webhook-Idempotency-Key";

    private final RecordingWebhookProperties properties;
    private final RecordingWebhookTargetValidator targetValidator;
    private final ObjectMapper objectMapper;
    private final Executor webhookExecutor;
    private final WebClient webhookClient;

    public RecordingWebhookService(
            RecordingWebhookProperties properties,
            RecordingWebhookTargetValidator targetValidator,
            ObjectMapper objectMapper,
            @Qualifier("webhookTaskExecutor") Executor webhookExecutor) {
        this.properties = properties;
        this.targetValidator = targetValidator;
        this.objectMapper = objectMapper;
        this.webhookExecutor = webhookExecutor;
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .responseTimeout(Duration.ofSeconds(10));
        this.webhookClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(256 * 1024))
                .build();
    }

    /**
     * Schedules delivery after confirm; safe to call from a transactional service (runs on a worker thread).
     */
    public void notifyRecordingReady(
            Long recordingId,
            Long sessionId,
            String s3Key,
            String fileName,
            Long fileSize,
            Integer durationSeconds,
            String contentType,
            LocalDateTime uploadedAt) {
        if (!properties.isConfigured()) {
            return;
        }
        URI callback;
        try {
            callback = properties.requireCallbackUri();
            targetValidator.validate(callback);
        } catch (Exception e) {
            log.warn("Recording webhook target invalid or unreachable for DNS check: {}", e.getMessage());
            return;
        }

        String idempotencyKey = "recording:" + recordingId;
        String occurredAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        Map<String, Object> recording = new LinkedHashMap<>();
        recording.put("id", recordingId);
        recording.put("sessionId", sessionId);
        recording.put("s3Key", s3Key);
        recording.put("fileName", fileName);
        recording.put("fileSize", fileSize);
        recording.put("durationSeconds", durationSeconds);
        recording.put("contentType", contentType);
        recording.put("uploadedAt", uploadedAt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("event", EVENT_RECORDING_READY);
        root.put("idempotencyKey", idempotencyKey);
        root.put("occurredAt", occurredAt);
        root.put("recording", recording);

        final String bodyJson;
        try {
            bodyJson = objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize recording webhook payload for recordingId={}", recordingId);
            return;
        }

        long timestampSeconds = Instant.now().getEpochSecond();
        String signature = sign(timestampSeconds, bodyJson, properties.getSigningSecret());
        String target = callback.toString();

        webhookExecutor.execute(() -> deliver(target, bodyJson, timestampSeconds, signature, idempotencyKey, recordingId));
    }

    private void deliver(
            String target,
            String bodyJson,
            long timestampSeconds,
            String signature,
            String idempotencyKey,
            Long recordingId) {
        try {
            var response = webhookClient.post()
                    .uri(URI.create(target))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HEADER_TIMESTAMP, Long.toString(timestampSeconds))
                    .header(HEADER_SIGNATURE, signature)
                    .header(HEADER_IDEMPOTENCY, idempotencyKey)
                    .bodyValue(bodyJson)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(15))
                    .onErrorResume(err -> {
                        log.warn("Recording webhook delivery failed recordingId={}: {}", recordingId, err.toString());
                        return Mono.empty();
                    })
                    .block();
            if (response != null) {
                log.info("Recording webhook delivered recordingId={} status={}", recordingId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Recording webhook delivery error recordingId={}: {}", recordingId, e.toString());
        }
    }

    static String sign(long timestampSeconds, String body, String secret) {
        try {
            String payload = timestampSeconds + "." + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "v1=" + HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC signing failed", e);
        }
    }
}
