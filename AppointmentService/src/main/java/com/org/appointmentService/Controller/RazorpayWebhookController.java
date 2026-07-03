package com.org.appointmentService.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.org.appointmentService.Services.PaymentService;
import com.org.appointmentService.Services.RazorpayLinkClient;

/**
 * Public endpoint (no JWT) — authenticity is established by verifying the
 * X-Razorpay-Signature HMAC over the raw body. Must be registered in the
 * Razorpay dashboard as {public-url}/appointment/webhook/razorpay with the
 * same webhook secret as RAZORPAY_WEBHOOK_SECRET.
 */
@RestController
@RequestMapping("/webhook")
public class RazorpayWebhookController {

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private RazorpayLinkClient razorpayLinkClient;

	private static final Logger logger = LoggerFactory.getLogger(RazorpayWebhookController.class);

	@PostMapping("/razorpay")
	public ResponseEntity<String> handleWebhook(
			@RequestBody String rawBody,
			@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

		if (!razorpayLinkClient.verifyWebhookSignature(rawBody, signature)) {
			logger.warn("Rejected Razorpay webhook with missing/invalid signature");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");
		}

		paymentService.processWebhook(rawBody);

		// Always 200 after signature check — retries won't fix processing
		// errors, and processing is idempotent anyway.
		return ResponseEntity.ok("ok");
	}
}
