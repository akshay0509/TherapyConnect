package com.org.appointmentService.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.org.appointmentService.Enums.PaymentStatus;

import lombok.Data;

@Data
public class PaymentInfoDto {

	private String appointmentId;
	private PaymentStatus status;
	private BigDecimal amount;
	private String currency;
	private String paymentLinkUrl;
	private String razorpayPaymentId;
	private boolean clientNotified;
	private String failureReason;
	private LocalDateTime updatedAt;
}
