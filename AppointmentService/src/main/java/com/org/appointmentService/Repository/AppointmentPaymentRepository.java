package com.org.appointmentService.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.appointmentService.Entity.AppointmentPayment;

public interface AppointmentPaymentRepository extends JpaRepository<AppointmentPayment, String> {

	Optional<AppointmentPayment> findByAppointmentId(String appointmentId);

	Optional<AppointmentPayment> findByAppointmentIdAndTherapistId(String appointmentId, String therapistId);

	Optional<AppointmentPayment> findByRazorpayLinkId(String razorpayLinkId);

	List<AppointmentPayment> findByAppointmentIdIn(List<String> appointmentIds);
}
