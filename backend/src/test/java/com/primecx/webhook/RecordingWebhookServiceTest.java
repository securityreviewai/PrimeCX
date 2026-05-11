package com.primecx.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class RecordingWebhookServiceTest {

    @Test
    void sign_matchesHmacSha256OverTimestampDotBody() throws Exception {
        String secret = "test-secret";
        long ts = 1_700_000_000L;
        String body = "{\"event\":\"recording.ready\"}";
        String signed = RecordingWebhookService.sign(ts, body, secret);

        assertThat(signed).startsWith("v1=");
        String hex = signed.substring("v1=".length());

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = mac.doFinal((ts + "." + body).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : expected) {
            sb.append(String.format("%02x", b));
        }
        assertThat(hex).isEqualTo(sb.toString());
    }
}
