package com.org.appointmentService.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.appointmentService.Dto.AvailabilityResponseDto;
import com.org.appointmentService.Dto.BookAppointmentRequest;
import com.org.appointmentService.Dto.RescheduleAppointmentRequest;
import com.org.appointmentService.Dto.UpdateAppointmentStatusRequest;
import com.org.appointmentService.Entity.TherapistAppointments;
import com.org.appointmentService.Entity.TherapistAvailability;
import com.org.appointmentService.Exception.AppointmentNotFoundException;
import com.org.appointmentService.Exception.InvalidAppointmentStatusTransitionException;
import com.org.appointmentService.Exception.SlotAlreadyBookedException;
import com.org.appointmentService.Exception.SlotNotAvailableException;
import com.org.appointmentService.Repository.TherapistAppointmentsRepository;
import com.org.appointmentService.Repository.TherapistAvailabilityRepository;
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
	private OutboxService outboxService;

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

	@Transactional
	public String bookAppointment(BookAppointmentRequest bookAppointmentRequest) throws JsonProcessingException {

		String slotId = bookAppointmentRequest.getSlotId();

		int updated = therapistAvailabilityRepository.markSlotAsBooked(slotId);

		if (updated == 0) {
			throw new SlotAlreadyBookedException();
		}

		TherapistAvailability therapistAvailability = therapistAvailabilityRepository.findBySlotIdAndTherapistId(slotId, bookAppointmentRequest.getTherapistId())
				.orElseThrow(() -> new SlotNotAvailableException(slotId));
		
		TherapistAppointments therapistAppointment = new TherapistAppointments();

		therapistAppointment.setSlotId(slotId);
		therapistAppointment.setTherapistId(bookAppointmentRequest.getTherapistId());
		therapistAppointment.setClientId(bookAppointmentRequest.getClientId());
		therapistAppointment.setClientName(bookAppointmentRequest.getClientName());
		therapistAppointment.setSessionType(bookAppointmentRequest.getSessionType());
		therapistAppointment.setStartTime(therapistAvailability.getStartTime());
		therapistAppointment.setEndTime(therapistAvailability.getEndTime());

		therapistAppointmentsRepository.save(therapistAppointment);
		
		AppointmentEvent appointmentEvent = new AppointmentEvent();
		appointmentEvent.setEventType("AppointmentCreated");
		appointmentEvent.setAppointmentId(therapistAppointment.getAppointmentId());
		appointmentEvent.setSlotId(slotId);
		appointmentEvent.setTherapistId(therapistAppointment.getTherapistId());
		appointmentEvent.setClientId(therapistAppointment.getClientId());
		appointmentEvent.setSessionType(therapistAppointment.getSessionType().toString());
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
		therapistAppointmentsRepository.save(therapistAppointment);

		if (targetStatus == AppointmentStatus.CANCELLED) {
			releaseSlotOrThrow(therapistAppointment.getSlotId(), "Cancelled appointment must release the booked slot.");
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

		int booked = therapistAvailabilityRepository.markSlotAsBooked(newSlotId);
		if (booked == 0) {
			throw new SlotNotAvailableException(newSlotId);
		}

		LocalDateTime oldStartTime = therapistAppointment.getStartTime();
		LocalDateTime oldEndTime = therapistAppointment.getEndTime();

		releaseSlotOrThrow(oldSlotId, "Previous slot must be released after reschedule.");

		therapistAppointment.setSlotId(newSlot.getSlotId());
		therapistAppointment.setStartTime(newSlot.getStartTime());
		therapistAppointment.setEndTime(newSlot.getEndTime());
		therapistAppointment.setStatus(AppointmentStatus.RESCHEDULED);

		therapistAppointmentsRepository.save(therapistAppointment);

		AppointmentEvent event = baseEventFromAppointment(therapistAppointment);
		event.setEventType("AppointmentRescheduled");
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

		return therapistAvailabilityRepository.findSlotsWithAppointment(therapistId);
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

	private void releaseSlotOrThrow(String slotId, String message) {
		int released = therapistAvailabilityRepository.markSlotAsAvailable(slotId);
		if (released == 0) {
			throw new InvalidAppointmentStatusTransitionException(message);
		}
	}

	private AppointmentEvent baseEventFromAppointment(TherapistAppointments appointment) {
		AppointmentEvent event = new AppointmentEvent();
		event.setAppointmentId(appointment.getAppointmentId());
		event.setSlotId(appointment.getSlotId());
		event.setTherapistId(appointment.getTherapistId());
		event.setClientId(appointment.getClientId());
		if (appointment.getSessionType() != null) {
			event.setSessionType(appointment.getSessionType().toString());
		}
		event.setStartTime(appointment.getStartTime());
		event.setEndTime(appointment.getEndTime());
		event.setBookingSource("THERAPIST");
		return event;
	}

}
