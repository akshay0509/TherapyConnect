package com.org.appointmentService.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "THERAPIST_PAYMENT_PROJECTION")
public class TherapistPaymentProjection {

	@Id
	private String therapistId;

	private boolean paymentEnabled;
}
