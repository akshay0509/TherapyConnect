package com.org.appointmentService.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.org.appointmentService.Dto.PaymentInfoDto;
import com.org.appointmentService.Services.PaymentService;
import com.org.appointmentService.Utility.SecurityUtils;

@RestController
@RequestMapping("/payments")
public class PaymentController {

	@Autowired
	private PaymentService paymentService;

	@GetMapping("/{appointmentId}")
	public ResponseEntity<PaymentInfoDto> getPayment(@PathVariable String appointmentId) {

		String therapistId = SecurityUtils.getTherapistId();

		return paymentService.getPaymentInfo(appointmentId, therapistId)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Creates the payment link if it doesn't exist yet, or retries after an
	 * earlier failure. Also the recovery path when link creation failed
	 * during booking.
	 */
	@PostMapping("/{appointmentId}/link")
	public ResponseEntity<PaymentInfoDto> ensureLink(@PathVariable String appointmentId) {

		String therapistId = SecurityUtils.getTherapistId();

		return paymentService.ensurePaymentLink(appointmentId, therapistId)
				.map(payment -> ResponseEntity.ok(paymentService.toDto(payment)))
				.orElse(ResponseEntity.unprocessableEntity().build());
	}
}
