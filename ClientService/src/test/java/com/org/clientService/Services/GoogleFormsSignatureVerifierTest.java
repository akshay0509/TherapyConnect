package com.org.clientService.Services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.web.server.ResponseStatusException;

class GoogleFormsSignatureVerifierTest {

    private static final Instant NOW = Instant.parse("2026-07-29T00:00:00Z");
    private static final String SECRET = "test-secret-that-is-long-enough-for-hmac";

    @Test
    void acceptsValidSignature() throws Exception {
        String body = "{\"responseId\":\"r1\"}";
        String timestamp = String.valueOf(NOW.getEpochSecond());
        GoogleFormsSignatureVerifier verifier = verifier();

        assertDoesNotThrow(() -> verifier.verify("int-a", timestamp, sign(timestamp + "." + body), body));
    }

    @Test
    void rejectsTamperedBody() throws Exception {
        String timestamp = String.valueOf(NOW.getEpochSecond());
        GoogleFormsSignatureVerifier verifier = verifier();

        assertThrows(ResponseStatusException.class,
                () -> verifier.verify("int-a", timestamp, sign(timestamp + ".original"), "changed"));
    }

    @Test
    void rejectsStaleTimestamp() throws Exception {
        String timestamp = String.valueOf(NOW.minusSeconds(301).getEpochSecond());
        GoogleFormsSignatureVerifier verifier = verifier();

        assertThrows(ResponseStatusException.class,
                () -> verifier.verify("int-a", timestamp, sign(timestamp + ".{}"), "{}"));
    }

    @Test
    void springCanInstantiateVerifierWithConfiguredConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    context,
                    "integration.google-forms.id=int-a",
                    "integration.google-forms.hmac-secret=" + SECRET,
                    "integration.google-forms.max-clock-skew-seconds=300");
            context.register(GoogleFormsSignatureVerifier.class);
            context.refresh();

            assertDoesNotThrow(() -> context.getBean(GoogleFormsSignatureVerifier.class));
        }
    }

    private GoogleFormsSignatureVerifier verifier() {
        return new GoogleFormsSignatureVerifier(
                "int-a", SECRET, Duration.ofMinutes(5), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
