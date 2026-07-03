package com.org.appointmentService.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.org.appointmentService.Enums.PaymentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "APPOINTMENT_PAYMENT")
public class AppointmentPayment {

	@Id
	private String paymentId;

	@Column(nullable = false, unique = true)
	private String appointmentId;

	@Column(nullable = false)
	private String therapistId;

	@Column(nullable = false)
	private BigDecimal amount;

	@Column(nullable = false)
	private String currency = "INR";

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PaymentStatus status = PaymentStatus.LINK_CREATED;

	private String razorpayLinkId;

	@Column(length = 1024)
	private String paymentLinkUrl;

	private String razorpayPaymentId;

	/** true when Razorpay sent the link to the client by SMS/email */
	private boolean clientNotified;

	@Column(length = 512)
	private String failureReason;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	public void onCreate() {
		if (this.paymentId == null) {
			String uniquePart = UUID.randomUUID().toString().substring(0, 8);
			this.paymentId = "PAY" + uniquePart;
		}
		this.createdAt = LocalDateTime.now();
		this.updatedAt = this.createdAt;
	}

	@PreUpdate
	public void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
