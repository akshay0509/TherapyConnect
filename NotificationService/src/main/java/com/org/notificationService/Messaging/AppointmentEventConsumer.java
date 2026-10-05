package com.org.notificationService.Messaging;

import java.time.ZoneId;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.org.events.TherapistAppointment.AppointmentEvent;
import com.org.notificationService.Entity.AppointmentCalendarEvent;
import com.org.notificationService.Entity.ClientProjection;
import com.org.notificationService.Entity.TherapistProjection;
import com.org.notificationService.Repository.AppointmentCalendarEventRepository;
import com.org.notificationService.Repository.ClientProjectionRepository;
import com.org.notificationService.Repository.TherapistProjectionRepository;
import com.org.notificationService.Services.GoogleCalendarService;

import jakarta.transaction.Transactional;

@Service
public class AppointmentEventConsumer {

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	GoogleCalendarService googleCalendarService;

	@Autowired
	ClientProjectionRepository clientProjectionRepository;

	@Autowired
	TherapistProjectionRepository therapistProjectionRepository;

	@Autowired
	AppointmentCalendarEventRepository appointmentCalendarEventRepository;

	// fallback only — the real email comes from TherapistProjection per therapist
	@Value("${google.calendar.therapist-email:}")
	private String fallbackTherapistEmail;

    @Autowired
    private com.org.notificationService.Services.MeetingStateService meetingStateService;

	private static final String topic = "therapist-appointment-events";
	private static final ZoneId FALLBACK_ZONE = ZoneId.of("Asia/Kolkata");

	private static final Logger logger = LoggerFactory.getLogger(AppointmentEventConsumer.class);

	// Failures must propagate: the container's DefaultErrorHandler retries
	// (5s x 2) and then dead-letters to <topic>.DLT, where the admin dashboard
	// can see and replay them. A catch-and-log here loses the event forever
	// (this is how expired-Google-token failures went unnoticed for days).
	@KafkaListener(topics = topic, groupId = "${spring.kafka.consumer.group-id}")
	@Transactional(rollbackOn = Exception.class)
	public void listen(JsonNode payload) throws Exception {

		logger.info("inside process of therapist-appointment-events..");

		String eventType = payload.get("eventType").asText();
		AppointmentEvent appointmentEvent = objectMapper.convertValue(payload, AppointmentEvent.class);

        if (!java.util.Set.of("AppointmentConfirmed", "AppointmentRescheduled", "AppointmentCancelled",
                "AppointmentCompleted", "AppointmentAbandoned").contains(eventType)) return;
        var meeting = meetingStateService.begin(appointmentEvent);
        if (meeting == null) return;
		switch (eventType) {

		case "AppointmentConfirmed" -> createInvite(appointmentEvent, meeting);
		case "AppointmentRescheduled" -> rescheduleInvite(appointmentEvent, meeting);
		case "AppointmentCancelled" -> cancelInvite(appointmentEvent, meeting);
		case "AppointmentCompleted", "AppointmentAbandoned" -> meetingStateService.clear(meeting);
		default -> logger.debug("Skipping unsupported appointment eventType={}", eventType);

		}
	}

	private ZoneId resolveZone(String therapistId) {
		Optional<TherapistProjection> projection = therapistProjectionRepository.findById(therapistId);
		if (projection.isEmpty()) {
			logger.warn("TherapistProjection not found for therapistId={}; using fallback timezone", therapistId);
			return FALLBACK_ZONE;
		}
		try {
			return ZoneId.of(projection.get().getTimezone());
		} catch (Exception e) {
			logger.warn("Invalid timezone '{}' for therapistId={}; using fallback", projection.get().getTimezone(), therapistId);
			return FALLBACK_ZONE;
		}
	}

	private String resolveTherapistEmail(String therapistId) {
		Optional<String> projectedEmail = therapistProjectionRepository.findById(therapistId)
				.map(TherapistProjection::getEmail)
				.filter(email -> !email.isBlank());
		if (projectedEmail.isPresent()) {
			return projectedEmail.get();
		}
		logger.warn("No email in TherapistProjection for therapistId={}; using configured fallback", therapistId);
		return fallbackTherapistEmail;
	}

	private String appointmentTitle(ClientProjection clientProjection) {
		String firstName = clientProjection.getFirstName() == null ? "" : clientProjection.getFirstName().trim();
		String lastName = clientProjection.getLastName() == null ? "" : clientProjection.getLastName().trim();
		String fullName = (firstName + " " + lastName).trim();
		return fullName.isEmpty() ? "Therapy Session" : "Therapy Session with " + fullName;
	}
	private void createInvite(AppointmentEvent appointmentEvent, com.org.notificationService.Entity.AppointmentMeetingState meeting) throws Exception {

		Optional<AppointmentCalendarEvent> existingCalendarEvent = appointmentCalendarEventRepository.findById(appointmentEvent.getAppointmentId());

		if (existingCalendarEvent.isPresent()) {
			// Preserve legacy duplicate-confirmation behavior: read the link without sending another invite update.
            if (appointmentEvent.getCalendarRevision() == null) {
                meetingStateService.capture(meeting, googleCalendarService.getAppointmentEvent(
                        existingCalendarEvent.get().getGoogleCalendarEventId()), appointmentEvent.getModeType());
                return;
            }
            // Versioned reconfirmation reconciles the current appointment.
            rescheduleInvite(appointmentEvent, meeting);
            return;
		}

		ClientProjection clientProjection = clientProjectionRepository.findById(appointmentEvent.getClientId())
				.orElseThrow(() -> new IllegalStateException(
						"Client projection not found for clientId=" + appointmentEvent.getClientId()));

		ZoneId zone = resolveZone(appointmentEvent.getTherapistId());

		String title = appointmentTitle(clientProjection);

		var googleEvent = googleCalendarService.createAppointmentEvent(
				clientProjection.getEmail(),
				resolveTherapistEmail(appointmentEvent.getTherapistId()),
				title,
				"Appointment ID: " + appointmentEvent.getAppointmentId(),
				appointmentEvent.getStartTime(),
				appointmentEvent.getEndTime(),
				appointmentEvent.getModeType(),
				appointmentEvent.getAddress(),
				zone
				);

		AppointmentCalendarEvent appointmentCalendarEvent = new AppointmentCalendarEvent();
		appointmentCalendarEvent.setAppointmentId(appointmentEvent.getAppointmentId());
		appointmentCalendarEvent.setGoogleCalendarEventId(googleEvent.getId());
		appointmentCalendarEventRepository.save(appointmentCalendarEvent);
        meetingStateService.capture(meeting, googleEvent, appointmentEvent.getModeType());
	}

	private void rescheduleInvite(AppointmentEvent appointmentEvent, com.org.notificationService.Entity.AppointmentMeetingState meeting) throws Exception {

		Optional<AppointmentCalendarEvent> existingCalendarEvent = appointmentCalendarEventRepository.findById(appointmentEvent.getAppointmentId());

		if (existingCalendarEvent.isEmpty()) {
			// Preserve production behavior: a missing mapping is never automatically repaired.
            // It may represent an orphaned Google event; creating another could duplicate invitations.
            if (Boolean.TRUE.equals(appointmentEvent.getCalendarEligible())) {
                meeting.setMeetingUrl(null); meeting.setMeetingStatus("FAILED");
                meetingStateService.save(meeting);
            } else {
                // False or unknown eligibility without a mapping never proves an invite existed.
                meetingStateService.clear(meeting);
            }
            return;
		}

		ClientProjection clientProjection = clientProjectionRepository.findById(appointmentEvent.getClientId())
				.orElseThrow(() -> new IllegalStateException(
						"Client projection not found for clientId=" + appointmentEvent.getClientId()));

		ZoneId zone = resolveZone(appointmentEvent.getTherapistId());

		String title = appointmentTitle(clientProjection);

		var googleEvent = googleCalendarService.updateAppointmentEvent(
				existingCalendarEvent.get().getGoogleCalendarEventId(),
				clientProjection.getEmail(),
				resolveTherapistEmail(appointmentEvent.getTherapistId()),
				title,
				"Appointment ID: " + appointmentEvent.getAppointmentId(),
				appointmentEvent.getStartTime(),
				appointmentEvent.getEndTime(),
				appointmentEvent.getModeType(),
				appointmentEvent.getAddress(),
				zone
				);
		meetingStateService.capture(meeting, googleEvent, appointmentEvent.getModeType());
	}

	private void cancelInvite(AppointmentEvent appointmentEvent, com.org.notificationService.Entity.AppointmentMeetingState meeting) throws Exception {

		Optional<AppointmentCalendarEvent> mapping = appointmentCalendarEventRepository.findById(appointmentEvent.getAppointmentId());
		if (mapping.isEmpty()) {
			meetingStateService.clear(meeting);
            logger.warn("Calendar mapping not found for cancel. appointmentId={}", appointmentEvent.getAppointmentId());
			return;
		}

		try {
			googleCalendarService.cancelAppointmentEvent(mapping.get().getGoogleCalendarEventId());
		}
		catch (GoogleJsonResponseException e) {
			// already deleted on Google's side — treat as success, clean up mapping
			if (e.getStatusCode() != 404 && e.getStatusCode() != 410) {
				throw e;
			}
			logger.warn("Calendar event already gone for appointmentId={}; removing mapping", appointmentEvent.getAppointmentId());
		}
		appointmentCalendarEventRepository.delete(mapping.get());
        meetingStateService.clear(meeting);
	}
}

