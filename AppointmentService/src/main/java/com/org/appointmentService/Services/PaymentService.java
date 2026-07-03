package com.org.appointmentService.Services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.appointmentService.Dto.PaymentInfoDto;
import com.org.appointmentService.Entity.AppointmentPayment;
import com.org.appointmentService.Entity.ClientContactProjection;
import com.org.appointmentService.Entity.TherapistAppointments;
import com.org.appointmentService.Enums.PaymentStatus;
import com.org.appointmentService.Repository.AppointmentPaymentRepository;
import com.org.appointmentService.Repository.ClientContactProjectionRepository;
import com.org.appointmentService.Repository.TherapistAppointmentsRepository;
import com.org.appointmentService.Repository.TherapistPaymentProjectionRepository;
import com.org.appointmentService.Repository.TherapyDeliveryModeRepository;
import com.org.events.TherapistAppointment.AppointmentEvent;
import com.org.events.TherapistAppointment.AppointmentStatus;

import jakarta.transaction.Transactional;

/**
 * Owns the Razorpay payment-link lifecycle for appointments.
 *
 * Design rules:
 * - Payment NEVER blocks or fails a booking. Every Razorpay call from the
 *   booking path is wrapped; failures are recorded on the payment row.
 * - If the therapist's paymentEnabled flag is off, or Razorpay keys are not
 *   configured, every method is a silent no-op.
 * - Webhook processing is idempotent: a PAID row is never transitioned again.
 */
@Service
public class PaymentService {

	@Autowired
	private AppointmentPaymentRepository appointmentPaymentRepository;

	@Autowired
	private TherapistPaymentProjectionRepository therapistPaymentProjectionRepository;

	@Autowired
	private ClientContactProjectionRepository clientContactProjectionRepository;

	@Autowired
	private TherapistAppointmentsRepository therapistAppointmentsRepository;

	@Autowired
	private TherapyDeliveryModeRepository therapyDeliveryModeRepository;

	@Autowired
	private RazorpayLinkClient razorpayLinkClient;

	@Autowired
	private OutboxService outboxService;

	@Autowired
	private ObjectMapper objectMapper;

	private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

	public boolean isPaymentEnabledForTherapist(String therapistId) {

		if (!razorpayLinkClient.isConfigured()) {
			return false;
		}

		return therapistPaymentProjectionRepository.findById(therapistId)
				.map(p -> p.isPaymentEnabled())
				.orElse(false);
	}

	/**
	 * Creates a payment link for the appointment if payments are enabled,
	 * or retries after an earlier failure. Never throws.
	 * Returns the payment row, or empty when payments do not apply.
	 */
	@Transactional
	public Optional<AppointmentPayment> ensurePaymentLink(String appointmentId, String therapistId) {

		if (!isPaymentEnabledForTherapist(therapistId)) {
			return Optional.empty();
		}

		Optional<TherapistAppointments> appointmentOpt =
				therapistAppointmentsRepository.findByAppointmentIdAndTherapistId(appointmentId, therapistId);

		if (appointmentOpt.isEmpty()) {
			logger.warn("ensurePaymentLink: appointment not found. appointmentId={}", appointmentId);
			return Optional.empty();
		}

		TherapistAppointments appointment = appointmentOpt.get();

		if (appointment.getStatus() == AppointmentStatus.CANCELLED
				|| appointment.getStatus() == AppointmentStatus.ABANDONED) {
			logger.info("ensurePaymentLink: appointment is terminal ({}); skipping. appointmentId={}",
					appointment.getStatus(), appointmentId);
			return Optional.empty();
		}

		BigDecimal fee = appointment.getSessionFee();
		if (fee == null || fee.signum() <= 0) {
			logger.info("ensurePaymentLink: no positive session fee; skipping. appointmentId={}", appointmentId);
			return Optional.empty();
		}

		AppointmentPayment payment = appointmentPaymentRepository.findByAppointmentId(appointmentId)
				.orElse(null);

		if (payment != null) {
			if (payment.getStatus() == PaymentStatus.PAID) {
				return Optional.of(payment);
			}
			if (payment.getStatus() == PaymentStatus.LINK_CREATED) {
				// active link already exists — reuse it
				return Optional.of(payment);
			}
			// LINK_FAILED / CANCELLED / EXPIRED → issue a fresh link below
		}
		else {
			payment = new AppointmentPayment();
			payment.setAppointmentId(appointmentId);
			payment.setTherapistId(therapistId);
		}

		payment.setAmount(fee);
		payment.setCurrency("INR");

		ClientContactProjection contact = clientContactProjectionRepository
				.findById(appointment.getClientId())
				.orElse(null);

		String description = "Therapy session " + appointmentId;

		try {
			JsonNode response;

			if (contact != null && hasAnyContact(contact)) {
				try {
					// Razorpay notifies the client by SMS/email with the link
					response = razorpayLinkClient.createPaymentLink(
							appointmentId, fee, description,
							buildName(contact), contact.getEmail(), contact.getPhoneNumber());
					payment.setClientNotified(true);
				}
				catch (Exception contactFailure) {
					// bad email/phone must not block the link — retry bare
					logger.warn("Payment link with customer notify failed for appointmentId={}; retrying without notify",
							appointmentId, contactFailure);
					response = razorpayLinkClient.createPaymentLink(
							appointmentId, fee, description, null, null, null);
					payment.setClientNotified(false);
				}
			}
			else {
				logger.info("No client contact on file for clientId={}; creating link without notify",
						appointment.getClientId());
				response = razorpayLinkClient.createPaymentLink(
						appointmentId, fee, description, null, null, null);
				payment.setClientNotified(false);
			}

			payment.setRazorpayLinkId(response.get("id").asText());
			payment.setPaymentLinkUrl(response.get("short_url").asText());
			payment.setStatus(PaymentStatus.LINK_CREATED);
			payment.setFailureReason(null);
		}
		catch (Exception e) {
			logger.error("Failed to create Razorpay payment link for appointmentId={}", appointmentId, e);
			payment.setStatus(PaymentStatus.LINK_FAILED);
			payment.setClientNotified(false);
			payment.setFailureReason(truncate(e.getMessage()));
		}

		return Optional.of(appointmentPaymentRepository.save(payment));
	}

	private boolean hasAnyContact(ClientContactProjection contact) {
		return (contact.getEmail() != null && !contact.getEmail().isBlank())
				|| (contact.getPhoneNumber() != null && !contact.getPhoneNumber().isBlank());
	}

	private String buildName(ClientContactProjection contact) {
		String name = ((contact.getFirstName() != null ? contact.getFirstName() : "") + " "
				+ (contact.getLastName() != null ? contact.getLastName() : "")).trim();
		return name.isBlank() ? null : name;
	}

	/**
	 * Cancels an active (unpaid) payment link. Best effort: local status is
	 * always updated so a later webhook can't confirm a dead appointment,
	 * even if the remote cancel call fails.
	 */
	@Transactional
	public void cancelUnpaidLinkBestEffort(String appointmentId) {

		appointmentPaymentRepository.findByAppointmentId(appointmentId).ifPresent(payment -> {

			if (payment.getStatus() != PaymentStatus.LINK_CREATED) {
				return;
			}

			try {
				razorpayLinkClient.cancelPaymentLink(payment.getRazorpayLinkId());
			}
			catch (Exception e) {
				logger.warn("Failed to cancel Razorpay link {} for appointmentId={} (continuing, local status set to CANCELLED)",
						payment.getRazorpayLinkId(), appointmentId, e);
			}

			payment.setStatus(PaymentStatus.CANCELLED);
			appointmentPaymentRepository.save(payment);
		});
	}

	/**
	 * After a reschedule the fee may have changed: cancel any active unpaid
	 * link and issue a fresh one at the current fee. A PAID payment is left
	 * untouched. Never throws.
	 */
	public void refreshLinkAfterReschedule(String appointmentId, String therapistId) {

		Optional<AppointmentPayment> existing = appointmentPaymentRepository.findByAppointmentId(appointmentId);

		if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.PAID) {
			return;
		}

		if (existing.isPresent()) {
			cancelUnpaidLinkBestEffort(appointmentId);
		}

		ensurePaymentLink(appointmentId, therapistId);
	}

	public Optional<PaymentInfoDto> getPaymentInfo(String appointmentId, String therapistId) {
		return appointmentPaymentRepository.findByAppointmentIdAndTherapistId(appointmentId, therapistId)
				.map(this::toDto);
	}

	/**
	 * Processes a verified Razorpay webhook body. Caller has already checked
	 * the signature. Unknown events and unknown links are acknowledged and
	 * ignored (returning an error would only make Razorpay retry forever).
	 */
	@Transactional
	public void processWebhook(String rawBody) {

		JsonNode root;
		try {
			root = objectMapper.readTree(rawBody);
		}
		catch (Exception e) {
			logger.error("Razorpay webhook body is not valid JSON; ignoring", e);
			return;
		}

		String event = root.path("event").asText("");

		switch (event) {
			case "payment_link.paid" -> handleLinkPaid(root);
			case "payment_link.cancelled" -> handleLinkClosed(root, PaymentStatus.CANCELLED);
			case "payment_link.expired" -> handleLinkClosed(root, PaymentStatus.EXPIRED);
			case "payment.failed" -> handlePaymentFailed(root);
			default -> logger.debug("Ignoring unsupported Razorpay webhook event={}", event);
		}
	}

	private void handleLinkPaid(JsonNode root) {

		JsonNode linkEntity = root.path("payload").path("payment_link").path("entity");
		String razorpayLinkId = linkEntity.path("id").asText(null);
		String referenceId = linkEntity.path("reference_id").asText(null);
		String razorpayPaymentId = root.path("payload").path("payment").path("entity").path("id").asText(null);

		AppointmentPayment payment = findPayment(razorpayLinkId, referenceId);
		if (payment == null) {
			logger.warn("payment_link.paid for unknown link. linkId={} referenceId={}", razorpayLinkId, referenceId);
			return;
		}

		if (payment.getStatus() == PaymentStatus.PAID) {
			// duplicate webhook delivery — idempotent
			return;
		}

		payment.setStatus(PaymentStatus.PAID);
		payment.setRazorpayPaymentId(razorpayPaymentId);
		payment.setFailureReason(null);
		appointmentPaymentRepository.save(payment);

		confirmAppointmentAfterPayment(payment);
	}

	private void handleLinkClosed(JsonNode root, PaymentStatus targetStatus) {

		JsonNode linkEntity = root.path("payload").path("payment_link").path("entity");
		String razorpayLinkId = linkEntity.path("id").asText(null);
		String referenceId = linkEntity.path("reference_id").asText(null);

		AppointmentPayment payment = findPayment(razorpayLinkId, referenceId);
		if (payment == null || payment.getStatus() == PaymentStatus.PAID) {
			return;
		}

		payment.setStatus(targetStatus);
		appointmentPaymentRepository.save(payment);
	}

	private void handlePaymentFailed(JsonNode root) {

		JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
		String referenceId = paymentEntity.path("notes").path("appointmentId").asText(null);
		String description = paymentEntity.path("error_description").asText("Payment failed");

		if (referenceId == null) {
			return;
		}

		appointmentPaymentRepository.findByAppointmentId(referenceId).ifPresent(payment -> {
			if (payment.getStatus() == PaymentStatus.LINK_CREATED) {
				// link stays active — the client can retry from the same link
				payment.setFailureReason(truncate(description));
				appointmentPaymentRepository.save(payment);
			}
		});
	}

	/**
	 * SCHEDULED/RESCHEDULED appointments are auto-confirmed once paid, and an
	 * AppointmentConfirmed event goes out so the calendar invite is created.
	 * If the therapist cancelled while the client was paying, the appointment
	 * is left terminal and the money is flagged for a manual refund.
	 */
	private void confirmAppointmentAfterPayment(AppointmentPayment payment) {

		TherapistAppointments appointment = therapistAppointmentsRepository
				.findByAppointmentIdAndTherapistId(payment.getAppointmentId(), payment.getTherapistId())
				.orElse(null);

		if (appointment == null) {
			logger.error("Payment PAID but appointment not found. appointmentId={}", payment.getAppointmentId());
			return;
		}

		if (appointment.getStatus() != AppointmentStatus.SCHEDULED
				&& appointment.getStatus() != AppointmentStatus.RESCHEDULED) {
			logger.warn("Payment received for appointment in status {} — manual refund may be required. appointmentId={}",
					appointment.getStatus(), appointment.getAppointmentId());
			return;
		}

		appointment.setStatus(AppointmentStatus.CONFIRMED);
		appointment.setStatusReason("Auto-confirmed on payment");
		therapistAppointmentsRepository.save(appointment);

		try {
			AppointmentEvent event = new AppointmentEvent();
			event.setEventType("AppointmentConfirmed");
			event.setAppointmentId(appointment.getAppointmentId());
			event.setSlotId(appointment.getSlotId());
			event.setTherapistId(appointment.getTherapistId());
			event.setClientId(appointment.getClientId());
			event.setSessionFee(appointment.getSessionFee());
			event.setModeId(appointment.getModeId());
			event.setStartTime(appointment.getStartTime());
			event.setEndTime(appointment.getEndTime());
			event.setBookingSource("THERAPIST");
			event.setReason("Auto-confirmed on payment");
			event.setUpdatedAt(LocalDateTime.now());

			therapyDeliveryModeRepository.findById(appointment.getModeId()).ifPresent(mode -> {
				event.setModeType(mode.getModeType().name());
				event.setAddress(mode.getAddress());
			});

			outboxService.saveOutboxEvent("THERAPIST_APPOINTMENT", appointment.getTherapistId(),
					"AppointmentConfirmed", event);
		}
		catch (Exception e) {
			// outbox failure must not lose the PAID status — surface loudly instead
			logger.error("Failed to write AppointmentConfirmed outbox event for appointmentId={}",
					appointment.getAppointmentId(), e);
		}
	}

	private AppointmentPayment findPayment(String razorpayLinkId, String referenceId) {

		if (razorpayLinkId != null) {
			Optional<AppointmentPayment> byLink = appointmentPaymentRepository.findByRazorpayLinkId(razorpayLinkId);
			if (byLink.isPresent()) {
				return byLink.get();
			}
		}

		if (referenceId != null) {
			return appointmentPaymentRepository.findByAppointmentId(referenceId).orElse(null);
		}

		return null;
	}

	public PaymentInfoDto toDto(AppointmentPayment payment) {
		PaymentInfoDto dto = new PaymentInfoDto();
		dto.setAppointmentId(payment.getAppointmentId());
		dto.setStatus(payment.getStatus());
		dto.setAmount(payment.getAmount());
		dto.setCurrency(payment.getCurrency());
		dto.setPaymentLinkUrl(payment.getPaymentLinkUrl());
		dto.setRazorpayPaymentId(payment.getRazorpayPaymentId());
		dto.setClientNotified(payment.isClientNotified());
		dto.setFailureReason(payment.getFailureReason());
		dto.setUpdatedAt(payment.getUpdatedAt());
		return dto;
	}

	private String truncate(String message) {
		if (message == null) {
			return null;
		}
		return message.length() > 500 ? message.substring(0, 500) : message;
	}
}
