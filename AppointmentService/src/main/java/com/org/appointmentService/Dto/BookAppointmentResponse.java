package com.org.appointmentService.Dto;

import com.org.appointmentService.Enums.PaymentStatus;

import lombok.Data;

@Data
public class BookAppointmentResponse {

	private String appointmentId;

	/** null when payments are disabled or do not apply to this booking */
	private PaymentStatus paymentStatus;
	private String paymentLinkUrl;

	/** true when Razorpay sent the link to the client by SMS/email */
	private boolean clientNotified;
}
