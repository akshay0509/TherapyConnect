package com.org.clientService.Services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GoogleFormsSignatureVerifier {

    private final String integrationId;
    private final String secret;
    private final Duration allowedSkew;
    private final Clock clock;

    @Autowired
    public GoogleFormsSignatureVerifier(
            @Value("${integration.google-forms.id:}") String integrationId,
            @Value("${integration.google-forms.hmac-secret:}") String secret,
            @Value("${integration.google-forms.max-clock-skew-seconds:300}") long allowedSkewSeconds) {
        this(integrationId, secret, Duration.ofSeconds(allowedSkewSeconds), Clock.systemUTC());
    }

    GoogleFormsSignatureVerifier(String integrationId, String secret, Duration allowedSkew, Clock clock) {
        this.integrationId = integrationId;
        this.secret = secret;
        this.allowedSkew = allowedSkew;
        this.clock = clock;
    }

    public void verify(String requestedIntegrationId, String timestampHeader, String signatureHeader, String body) {
        if (integrationId.isBlank() || secret.isBlank() || !integrationId.equals(requestedIntegrationId)) {
            throw unauthorized("Integration is not configured.");
        }

        final long epochSeconds;
        try {
            epochSeconds = Long.parseLong(timestampHeader);
        } catch (RuntimeException ex) {
            throw unauthorized("Invalid integration timestamp.");
        }

        Duration age = Duration.between(Instant.ofEpochSecond(epochSeconds), clock.instant()).abs();
        if (age.compareTo(allowedSkew) > 0) {
            throw unauthorized("Integration request has expired.");
        }

        byte[] expected = hmac(timestampHeader + "." + body);
        final byte[] supplied;
        try {
            supplied = HexFormat.of().parseHex(signatureHeader == null ? "" : signatureHeader.trim());
        } catch (IllegalArgumentException ex) {
            throw unauthorized("Invalid integration signature.");
        }

        if (!MessageDigest.isEqual(expected, supplied)) {
            throw unauthorized("Invalid integration signature.");
        }
    }

    private byte[] hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to validate integration signature.", ex);
        }
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
