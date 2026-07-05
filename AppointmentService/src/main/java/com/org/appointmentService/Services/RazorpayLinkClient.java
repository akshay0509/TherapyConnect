package com.org.appointmentService.Services;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Thin wrapper around the Razorpay Payment Links REST API.
 * Uses key-based Basic auth; no SDK dependency.
 * If keys are not configured, isConfigured() returns false and callers
 * must treat payments as globally disabled.
 */
@Component
public class RazorpayLinkClient {

	@Value("${razorpay.key-id:}")
	private String keyId;

	@Value("${razorpay.key-secret:}")
	private String keySecret;

	@Value("${razorpay.webhook-secret:}")
	private String webhookSecret;

	private static final String BASE_URL = "https://api.razorpay.com/v1";

	// Timeouts are mandatory: a default RestTemplate waits forever, and a
	// blackholed Razorpay would pin every booking thread. A timeout here
	// surfaces as the already-handled LINK_FAILED path.
	private final RestTemplate restTemplate = buildRestTemplate();

	private static final Logger logger = LoggerFactory.getLogger(RazorpayLinkClient.class);

	private static RestTemplate buildRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(3000);
		factory.setReadTimeout(5000);
		return new RestTemplate(factory);
	}

	public boolean isConfigured() {
		return keyId != null && !keyId.isBlank()
				&& keySecret != null && !keySecret.isBlank();
	}

	public boolean isWebhookConfigured() {
		return webhookSecret != null && !webhookSecret.isBlank();
	}

	/**
	 * Creates a payment link. Amount is in INR; converted to paise for Razorpay.
	 * When customer email/phone is provided, Razorpay itself notifies the
	 * client by email/SMS with the link.
	 * Returns the response JSON (contains id, short_url, status).
	 * Throws on any transport/API error — caller decides how to degrade.
	 */
	public JsonNode createPaymentLink(String appointmentId, BigDecimal amountInr, String description,
			String customerName, String customerEmail, String customerPhone) {

		long amountPaise = amountInr.multiply(BigDecimal.valueOf(100)).longValueExact();

		Map<String, Object> body = new HashMap<>();
		body.put("amount", amountPaise);
		body.put("currency", "INR");
		body.put("reference_id", appointmentId);
		body.put("description", description);
		body.put("reminder_enable", true);
		body.put("notes", Map.of("appointmentId", appointmentId));

		String contact = normalizePhone(customerPhone);
		boolean hasEmail = customerEmail != null && !customerEmail.isBlank();
		boolean hasContact = contact != null;

		if (hasEmail || hasContact) {
			Map<String, Object> customer = new HashMap<>();
			if (customerName != null && !customerName.isBlank()) {
				customer.put("name", customerName);
			}
			if (hasEmail) {
				customer.put("email", customerEmail);
			}
			if (hasContact) {
				customer.put("contact", contact);
			}
			body.put("customer", customer);
			body.put("notify", Map.of("sms", hasContact, "email", hasEmail));
		}

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, authHeaders());
		return restTemplate.postForObject(BASE_URL + "/payment_links", request, JsonNode.class);
	}

	/**
	 * Razorpay rejects contacts with spaces/dashes. Strips formatting and
	 * defaults bare 10-digit numbers to +91. Returns null when the number
	 * can't be salvaged — caller then omits SMS notify.
	 */
	static String normalizePhone(String phone) {

		if (phone == null || phone.isBlank()) {
			return null;
		}

		String cleaned = phone.replaceAll("[^0-9+]", "");
		if (cleaned.startsWith("+")) {
			cleaned = "+" + cleaned.substring(1).replace("+", "");
		}

		if (cleaned.matches("\\+\\d{11,14}")) {
			return cleaned;
		}
		if (cleaned.matches("\\d{10}")) {
			return "+91" + cleaned;
		}
		return null;
	}

	/**
	 * Cancels a payment link. Only links in 'created' state can be cancelled;
	 * Razorpay returns an error otherwise — caller should treat this as best effort.
	 */
	public void cancelPaymentLink(String razorpayLinkId) {
		HttpEntity<Void> request = new HttpEntity<>(authHeaders());
		restTemplate.postForObject(BASE_URL + "/payment_links/" + razorpayLinkId + "/cancel", request, JsonNode.class);
	}

	/**
	 * Verifies the X-Razorpay-Signature header: HMAC-SHA256 of the raw webhook
	 * body using the webhook secret, hex-encoded.
	 */
	public boolean verifyWebhookSignature(String rawBody, String signature) {

		if (!isWebhookConfigured() || signature == null || signature.isBlank()) {
			return false;
		}

		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));

			StringBuilder hex = new StringBuilder();
			for (byte b : digest) {
				hex.append(String.format("%02x", b));
			}

			// constant-time comparison
			return java.security.MessageDigest.isEqual(
					hex.toString().getBytes(StandardCharsets.UTF_8),
					signature.getBytes(StandardCharsets.UTF_8));
		}
		catch (Exception e) {
			logger.error("Failed to verify Razorpay webhook signature", e);
			return false;
		}
	}

	private HttpHeaders authHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String credentials = Base64.getEncoder()
				.encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
		headers.set("Authorization", "Basic " + credentials);
		return headers;
	}
}
