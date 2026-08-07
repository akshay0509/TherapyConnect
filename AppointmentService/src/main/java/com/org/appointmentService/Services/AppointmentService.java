package com.org.appointmentService.Services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.appointmentService.Dto.AppointmentScheduleAppointmentDto;
import com.org.appointmentService.Dto.AppointmentScheduleOverrideDto;
import com.org.appointmentService.Dto.AppointmentScheduleViewDto;
import com.org.appointmentService.Dto.AvailabilityResponseDto;
import com.org.appointmentService.Dto.BookAppointmentRequest;
import com.org.appointmentService.Dto.RescheduleAppointmentRequest;
import com.org.appointmentService.Dto.UpdateAppointmentStatusRequest;
import com.org.appointmentService.Entity.AppointmentPayment;
import com.org.appointmentService.Entity.ClientContactProjection;
import com.org.appointmentService.Entity.TherapistAppointments;
import com.org.appointmentService.Entity.TherapistAvailability;
import com.org.appointmentService.Entity.TherapistAvailabilityOverride;
import com.org.appointmentService.Entity.TherapistServiceProjection;
import com.org.appointmentService.Entity.TherapyDeliveryMode;
import com.org.appointmentService.Exception.AppointmentNotFoundException;
import com.org.appointmentService.Exception.InvalidAppointmentStatusTransitionException;
import com.org.appointmentService.Exception.SlotNotAvailableException;
import com.org.appointmentService.Repository.AppointmentPaymentRepository;
import com.org.appointmentService.Repository.TherapistAppointmentsRepository;
import com.org.appointmentService.Repository.TherapistAvailabilityOverrideRepository;
import com.org.appointmentService.Repository.TherapistAvailabilityRepository;
import com.org.appointmentService.Repository.ClientContactProjectionRepository;
import com.org.appointmentService.Repository.TherapistServiceProjectionRepository;
import com.org.appointmentService.Repository.TherapyDeliveryModeRepository;
import com.org.events.TherapistAppointment.AppointmentEvent;
import com.org.events.TherapistAppointment.AppointmentStatus;

import jakarta.transaction.Transactional;

@Service
public class AppointmentService {

	@Autowired
	private TherapistAvailabilityRepository therapistAvailabilityRepository;

	@Autowired
	private TherapistAppointmentsRepository therapistAppointmentsRepository;

	@Autowired
	private TherapistAvailabilityOverrideRepository therapistAvailabilityOverrideRepository;

	@Autowired
	private TherapyDeliveryModeRepository therapyDeliveryModeRepository;

	@Autowired
	private OutboxService outboxService;

	@Autowired
	private AppointmentPaymentRepository appointmentPaymentRepository;

	@Autowired
	private TherapistServiceProjectionRepository therapistServiceProjectionRepository;

	@Autowired
	private ClientContactProjectionRepository clientContactProjectionRepository;

	@Autowired
	private GenericBlockReservationService genericBlockReservationService;

	private static final EnumSet<AppointmentStatus> TERMINAL_STATUSES = EnumSet.of(
			AppointmentStatus.COMPLETED,
			AppointmentStatus.CANCELLED,
			AppointmentStatus.ABANDONED
			);

	private static final EnumSet<AppointmentStatus> MANUAL_TARGET_STATUSES = EnumSet.of(
			AppointmentStatus.CONFIRMED,
			AppointmentStatus.COMPLETED,
			AppointmentStatus.CANCELLED,
			AppointmentStatus.ABANDONED
			);

	/**
	 * Resolves AND validates the fee for a booking. Every path either returns a
	 * usable amount or throws — there is no separate validation step, because a
	 * second pass could only re-derive what this method already knows.
	 *
	 * Precedence, highest first:
	 *   0. the client is pro bono (DSF) — always zero, nothing overrides it
	 *   1. a custom fee typed at booking time
	 *   2. the client's negotiated flat rate, if one is set
	 *   3. the delivery mode's standard price
	 *
	 * The resolved value is stamped onto the appointment, so later changes to a
	 * client's rate — or to the DSF flag itself — never alter an existing
	 * booking. That immutability is what earnings depends on: it sums the stamped
	 * fee rather than joining the client's current flag, so a therapist ending a
	 * pro-bono arrangement cannot retroactively turn free sessions into income.
	 *
	 * Hence the invariant the earnings queries rely on: sessionFee == 0 if and
	 * only if the session was pro bono. It holds structurally here — the DSF
	 * branch is the only one that can return zero, and every other branch is
	 * checked for a positive amount before it returns. A null or non-positive fee
	 * reaching the database would be financially broken anyway: earnings would
	 * count it and the Razorpay link amount would be invalid.
	 */
	private BigDecimal resolveSessionFee(String clientId, BigDecimal customPrice, TherapyDeliveryMode deliveryMode) {
		// Request input, so it is checked where it enters rather than downstream
		// where the reason for the failure is no longer obvious.
		if (customPrice != null && customPrice.signum() <= 0) {
			throw new IllegalArgumentException("A custom session fee must be a positive amount.");
		}

		ClientContactProjection client = clientContactProjectionRepository.findById(clientId).orElse(null);

		if (client != null && Boolean.TRUE.equals(client.getDsf())) {
			return BigDecimal.ZERO;   // the only legitimate zero
		}
		if (customPrice != null) {
			return customPrice;       // already proven positive above
		}

		BigDecimal clientRate = client == null ? null : client.getSessionFee();
		BigDecimal fee = clientRate != null && clientRate.signum() > 0 ? clientRate : deliveryMode.getPrice();
		if (fee == null || fee.signum() <= 0) {
			throw new IllegalArgumentException(
					"Session fee must be a positive amount — set a valid price on the delivery mode.");
		}
		return fee;
	}

	@Transactional
	public String bookAppointment(BookAppointmentRequest bookAppointmentRequest) throws JsonProcessingException {

		String slotId = bookAppointmentRequest.getSlotId();
		String modeId = bookAppointmentRequest.getModeId();

		TherapistAvailability therapistAvailability = therapistAvailabilityRepository.findBySlotIdAndTherapistId(slotId, bookAppointmentRequest.getTherapistId())
				.orElseThrow(() -> new SlotNotAvailableException(slotId));

		TherapyDeliveryMode deliveryMode = therapyDeliveryModeRepository
				.findByModeIdAndTherapistIdAndIsActiveTrue(
						modeId,
						bookAppointmentRequest.getTherapistId())
				.orElseThrow(() -> new SlotNotAvailableException(
						"Mode " + modeId + " is not available for slot " + slotId));

		TherapistServiceProjection service = resolveActiveService(
				deliveryMode.getServiceId(),
				bookAppointmentRequest.getTherapistId());
		LocalDateTime appointmentEnd = therapistAvailability.getStartTime()
				.plusMinutes(service.getDurationMinutes());

		BigDecimal sessionFee = resolveSessionFee(
				bookAppointmentRequest.getClientId(),
				bookAppointmentRequest.getCustomPrice(),
				deliveryMode);


		if (isBlockedByUnavailableOverride(
				bookAppointmentRequest.getTherapistId(),
				therapistAvailability.getStartTime(),
				appointmentEnd)) {
			throw new SlotNotAvailableException(slotId);
		}

		if (therapistAppointmentsRepository.existsActiveAppointmentOverlap(
				bookAppointmentRequest.getTherapistId(),
				null,
				therapistAvailability.getStartTime(),
				appointmentEnd)) {
			throw new SlotNotAvailableException(
					"Requested time overlaps an existing appointment.");
		}

		TherapistAppointments therapistAppointment = new TherapistAppointments();

		therapistAppointment.setSlotId(slotId);
		therapistAppointment.setTherapistId(bookAppointmentRequest.getTherapistId());
		therapistAppointment.setClientId(bookAppointmentRequest.getClientId());
		therapistAppointment.setClientName(bookAppointmentRequest.getClientName());
		therapistAppointment.setSessionFee(sessionFee);
		therapistAppointment.setModeId(modeId);
		therapistAppointment.setServiceId(service.getServiceId());
		therapistAppointment.setStartTime(therapistAvailability.getStartTime());
		therapistAppointment.setEndTime(appointmentEnd);

		therapistAppointmentsRepository.saveAndFlush(therapistAppointment);
		genericBlockReservationService.reserve(
				therapistAppointment.getTherapistId(),
				therapistAppointment.getStartTime(),
				therapistAppointment.getEndTime(),
				therapistAppointment.getAppointmentId());

		AppointmentEvent appointmentEvent = new AppointmentEvent();
		appointmentEvent.setEventType("AppointmentCreated");
		appointmentEvent.setAppointmentId(therapistAppointment.getAppointmentId());
		appointmentEvent.setSlotId(slotId);
		appointmentEvent.setSessionFee(therapistAppointment.getSessionFee());
		appointmentEvent.setTherapistId(therapistAppointment.getTherapistId());
		appointmentEvent.setClientId(therapistAppointment.getClientId());
		appointmentEvent.setModeId(therapistAppointment.getModeId());
		appointmentEvent.setServiceId(therapistAppointment.getServiceId());
		appointmentEvent.setModeType(deliveryMode.getModeType().name());
		appointmentEvent.setAddress(deliveryMode.getAddress());
		appointmentEvent.setStartTime(therapistAppointment.getStartTime());
		appointmentEvent.setEndTime(therapistAppointment.getEndTime());
		appointmentEvent.setBookingSource("THERAPIST");

		outboxService.saveOutboxEvent("THERAPIST_APPOINTMENT", therapistAppointment.getTherapistId(), "AppointmentCreated", appointmentEvent);

		return therapistAppointment.getAppointmentId();

	}

	@Transactional
	public void updateAppointmentStatus(UpdateAppointmentStatusRequest updateAppointmentStatusRequest) throws JsonProcessingException {

		String appointmentId = updateAppointmentStatusRequest.getAppointmentId();
		String therapistId = updateAppointmentStatusRequest.getTherapistId();

		TherapistAppointments therapistAppointment = therapistAppointmentsRepository.findByAppointmentIdAndTherapistId(appointmentId, therapistId)
				.orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

		AppointmentStatus currentStatus = therapistAppointment.getStatus();
		AppointmentStatus targetStatus = updateAppointmentStatusRequest.getStatus();

		if (!MANUAL_TARGET_STATUSES.contains(targetStatus)) {
			throw new InvalidAppointmentStatusTransitionException(
					"Only CONFIRMED/COMPLETED/CANCELLED/ABANDONED are allowed for manual status update.");
		}

		if (TERMINAL_STATUSES.contains(therapistAppointment.getStatus())) {
			throw new InvalidAppointmentStatusTransitionException(
					"Cannot update terminal appointment status from " + therapistAppointment.getStatus());
		}

		validateStatusTransition(currentStatus, targetStatus);

		therapistAppointment.setStatus(targetStatus);
		therapistAppointment.setStatusReason(updateAppointmentStatusRequest.getReason());
		therapistAppointmentsRepository.save(therapistAppointment);

		if (targetStatus == AppointmentStatus.CANCELLED) {
			genericBlockReservationService.release(
					therapistAppointment.getTherapistId(),
					therapistAppointment.getStartTime(),
					therapistAppointment.getEndTime(),
					therapistAppointment.getAppointmentId());
		}

		AppointmentEvent event = baseEventFromAppointment(therapistAppointment);
		event.setReason(updateAppointmentStatusRequest.getReason());
		event.setUpdatedAt(LocalDateTime.now());
		event.setEventType(mapEventType(targetStatus));

		outboxService.saveOutboxEvent("THERAPIST_APPOINTMENT", therapistAppointment.getTherapistId(), event.getEventType(), event);
	}

	@Transactional
	public void rescheduleAppointment(RescheduleAppointmentRequest rescheduleAppointmentRequest) throws JsonProcessingException {

		String appointmentId = rescheduleAppointmentRequest.getAppointmentId();
		String therapistId = rescheduleAppointmentRequest.getTherapistId();
		String newSlotId = rescheduleAppointmentRequest.getNewSlotId();

		TherapistAppointments therapistAppointment = therapistAppointmentsRepository.findByAppointmentIdAndTherapistId(appointmentId, therapistId)
				.orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

		if (TERMINAL_STATUSES.contains(therapistAppointment.getStatus())) {
			throw new InvalidAppointmentStatusTransitionException(
					"Cannot reschedule terminal appointment with status " + therapistAppointment.getStatus());
		}

		String oldSlotId = therapistAppointment.getSlotId();
		if (oldSlotId.equals(newSlotId)) {
			throw new InvalidAppointmentStatusTransitionException("New slot must be different from current slot.");
		}

		TherapistAvailability newSlot = therapistAvailabilityRepository.findBySlotIdAndTherapistId(newSlotId, therapistId)
				.orElseThrow(() -> new SlotNotAvailableException(newSlotId));

		String modeId = rescheduleAppointmentRequest.getModeId() != null
				? rescheduleAppointmentRequest.getModeId()
				: therapistAppointment.getModeId();

		TherapyDeliveryMode deliveryMode = therapyDeliveryModeRepository
				.findByModeIdAndTherapistIdAndIsActiveTrue(modeId, therapistId)
				.orElseThrow(() -> new SlotNotAvailableException(
						"Mode " + modeId + " is not available for slot " + newSlotId));

		TherapistServiceProjection service =
				resolveActiveService(deliveryMode.getServiceId(), therapistId);
		LocalDateTime newEndTime =
				newSlot.getStartTime().plusMinutes(service.getDurationMinutes());

		// Rescheduling moves the time, not the price: keep the fee captured at
		// booking so a negotiated or custom rate survives. Only a genuine mode
		// change re-resolves. Previously this always reset to the mode price,
		// silently wiping any custom fee on reschedule.
		BigDecimal sessionFee =
				deliveryMode.getModeId().equals(therapistAppointment.getModeId())
						&& therapistAppointment.getSessionFee() != null
					? therapistAppointment.getSessionFee()
					: resolveSessionFee(therapistAppointment.getClientId(), null, deliveryMode);

		if (isBlockedByUnavailableOverride(
				therapistId, newSlot.getStartTime(), newEndTime)) {
			throw new SlotNotAvailableException(newSlotId);
		}

		// same double-booking guard as bookAppointment; the appointment being
		// rescheduled must not block its own move
		if (therapistAppointmentsRepository.existsActiveAppointmentOverlap(
				therapistId,
				appointmentId,
				newSlot.getStartTime(),
				newEndTime)) {
			throw new SlotNotAvailableException(
					"Requested time overlaps an existing appointment.");
		}

		LocalDateTime oldStartTime = therapistAppointment.getStartTime();
		LocalDateTime oldEndTime = therapistAppointment.getEndTime();

		genericBlockReservationService.move(
				therapistId,
				oldStartTime,
				oldEndTime,
				newSlot.getStartTime(),
				newEndTime,
				appointmentId);

		therapistAppointment.setSlotId(newSlot.getSlotId());
		therapistAppointment.setSessionFee(sessionFee);
		therapistAppointment.setModeId(modeId);
		therapistAppointment.setServiceId(service.getServiceId());
		therapistAppointment.setStartTime(newSlot.getStartTime());
		therapistAppointment.setEndTime(newEndTime);
		therapistAppointment.setStatus(AppointmentStatus.RESCHEDULED);
		therapistAppointment.setStatusReason(rescheduleAppointmentRequest.getReason());

		therapistAppointmentsRepository.save(therapistAppointment);

		AppointmentEvent event = baseEventFromAppointment(therapistAppointment);
		event.setEventType("AppointmentRescheduled");
		event.setModeType(deliveryMode.getModeType().name());
		event.setAddress(deliveryMode.getAddress());
		event.setOldSlotId(oldSlotId);
		event.setNewSlotId(newSlot.getSlotId());
		event.setOldStartTime(oldStartTime);
		event.setOldEndTime(oldEndTime);
		event.setReason(rescheduleAppointmentRequest.getReason());
		event.setUpdatedAt(LocalDateTime.now());

		outboxService.saveOutboxEvent("THERAPIST_APPOINTMENT", therapistAppointment.getTherapistId(), "AppointmentRescheduled", event);
	}

	public List<TherapistAppointments> getTherapistAppointments(String therapistId){

		LocalDate today = LocalDate.now();

		LocalDateTime startOfDay = today.atStartOfDay();
		LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

		return therapistAppointmentsRepository.findByTherapistIdAndStatusInAndStartTimeBetweenOrderByStartTimeAsc(
				therapistId,
				List.of(
						AppointmentStatus.CONFIRMED,
						AppointmentStatus.RESCHEDULED
				),
				startOfDay,
				endOfDay);
	}

	public List<AvailabilityResponseDto> getTherapistAvailabilityWithAppointments(String therapistId){
		return therapistAvailabilityRepository.findEffectiveSlotsWithAppointment(therapistId);
	}

	public AppointmentScheduleViewDto getAppointmentEditorView(String therapistId, LocalDate fromDate, LocalDate toDate) {
		LocalDateTime from = fromDate.atStartOfDay();
		LocalDateTime to = toDate.plusDays(1).atStartOfDay();

		List<AvailabilityResponseDto> slots = therapistAvailabilityRepository.findEffectiveSlotsWithAppointmentInRange(therapistId, from, to);

		List<AppointmentScheduleAppointmentDto> appointments = withPaymentInfo(therapistAppointmentsRepository
				.findByTherapistIdAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(therapistId, to, from).stream()
				.map(this::toAppointmentScheduleAppointmentDto)
				.toList());

		List<AppointmentScheduleOverrideDto> overrides = therapistAvailabilityOverrideRepository
				.findByTherapistIdAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(therapistId, to, from).stream()
				.map(this::toAppointmentScheduleOverrideDto)
				.toList();

		return new AppointmentScheduleViewDto(slots, appointments, overrides);
	}


	private List<AppointmentScheduleAppointmentDto> withPaymentInfo(List<AppointmentScheduleAppointmentDto> appointments) {

		if (appointments.isEmpty()) {
			return appointments;
		}

		List<String> appointmentIds = appointments.stream()
				.map(AppointmentScheduleAppointmentDto::getAppointmentId)
				.toList();

		Map<String, AppointmentPayment> paymentsByAppointment = appointmentPaymentRepository
				.findByAppointmentIdIn(appointmentIds).stream()
				.collect(Collectors.toMap(AppointmentPayment::getAppointmentId, p -> p));

		for (AppointmentScheduleAppointmentDto dto : appointments) {
			AppointmentPayment payment = paymentsByAppointment.get(dto.getAppointmentId());
			if (payment != null) {
				dto.setPaymentStatus(payment.getStatus());
				dto.setPaymentLinkUrl(payment.getPaymentLinkUrl());
			}
		}

		return appointments;
	}

	private AppointmentScheduleAppointmentDto toAppointmentScheduleAppointmentDto(TherapistAppointments appointment) {
		AppointmentScheduleAppointmentDto dto = new AppointmentScheduleAppointmentDto();
		dto.setAppointmentId(appointment.getAppointmentId());
		dto.setClientId(appointment.getClientId());
		dto.setClientName(appointment.getClientName());
		dto.setStartTime(appointment.getStartTime());
		dto.setEndTime(appointment.getEndTime());
		dto.setStatus(appointment.getStatus());
		dto.setModeId(appointment.getModeId());
		dto.setServiceId(appointment.getServiceId());
		dto.setReason(appointment.getStatusReason());
		return dto;
	}


	private AppointmentScheduleOverrideDto toAppointmentScheduleOverrideDto(TherapistAvailabilityOverride override) {
		AppointmentScheduleOverrideDto dto = new AppointmentScheduleOverrideDto();
		dto.setOverrideId(override.getOverrideId());
		dto.setTherapistId(override.getTherapistId());
		dto.setStartTime(override.getStartTime());
		dto.setEndTime(override.getEndTime());
		dto.setAvailable(override.isAvailable());
		dto.setReason(override.getReason());
		return dto;
	}

	private boolean isBlockedByUnavailableOverride(String therapistId, LocalDateTime slotStart, LocalDateTime slotEnd) {
		return !therapistAvailabilityOverrideRepository
				.findByTherapistIdAndAvailableFalseAndStartTimeLessThanAndEndTimeGreaterThan(therapistId, slotEnd, slotStart)
				.isEmpty();
	}

	private void validateStatusTransition(AppointmentStatus currentStatus, AppointmentStatus targetStatus) {

		if (currentStatus == targetStatus) {
			throw new InvalidAppointmentStatusTransitionException(
					"Appointment is already in status " + currentStatus);
		}

		if (targetStatus == AppointmentStatus.CONFIRMED) {
			if (currentStatus != AppointmentStatus.SCHEDULED && currentStatus != AppointmentStatus.RESCHEDULED) {
				throw new InvalidAppointmentStatusTransitionException(
						"Only SCHEDULED or RESCHEDULED appointments can be confirmed.");
			}
			return;
		}
	}

	private String mapEventType(AppointmentStatus targetStatus) {
		return switch (targetStatus) {
		case CONFIRMED -> "AppointmentConfirmed";
		case COMPLETED -> "AppointmentCompleted";
		case CANCELLED -> "AppointmentCancelled";
		case ABANDONED -> "AppointmentAbandoned";
		default -> throw new InvalidAppointmentStatusTransitionException(
				"Unsupported manual target status: " + targetStatus);
		};
	}

	private AppointmentEvent baseEventFromAppointment(TherapistAppointments appointment) {
		AppointmentEvent event = new AppointmentEvent();
		event.setAppointmentId(appointment.getAppointmentId());
		event.setSlotId(appointment.getSlotId());
		event.setTherapistId(appointment.getTherapistId());
		event.setClientId(appointment.getClientId());
		event.setSessionFee(appointment.getSessionFee());
		event.setModeId(appointment.getModeId());
		event.setServiceId(appointment.getServiceId());
		event.setStartTime(appointment.getStartTime());
		event.setEndTime(appointment.getEndTime());
		event.setBookingSource("THERAPIST");
		therapyDeliveryModeRepository.findById(appointment.getModeId()).ifPresent(mode -> {
			event.setModeType(mode.getModeType().name());
			event.setAddress(mode.getAddress());
		});
		return event;
	}

	private TherapistServiceProjection resolveActiveService(
			String serviceId,
			String therapistId) {
		TherapistServiceProjection service = therapistServiceProjectionRepository
				.findByServiceIdAndTherapistIdAndActiveTrue(serviceId, therapistId)
				.orElseThrow(() -> new SlotNotAvailableException(
						"Service definition is missing or inactive for service " + serviceId));
		// No multiple-of-30 requirement: a session reserves whole blocks rounded
		// up, so a 50-minute service simply holds a 60-minute footprint.
		if (service.getDurationMinutes() <= 0) {
			throw new SlotNotAvailableException(
					"Service " + serviceId + " has no usable duration.");
		}
		return service;
	}

}
