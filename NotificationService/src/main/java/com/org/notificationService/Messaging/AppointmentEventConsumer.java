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

	private static final String topic = "therapist-appointment-events";
	private static final ZoneId FALLBACK_ZONE = ZoneId.of("Asia/Kolkata");

	private static final Logger logger = LoggerFactory.getLogger(AppointmentEventConsumer.class);

	// Failures must propagate: the container's DefaultErrorHandler retries
	// (5s x 2) and then dead-letters to <topic>.DLT, where the admin dashboard
	// can see and replay them. A catch-and-log here loses the event forever
	// (this is how expired-Google-token failures went unnoticed for days).
	@KafkaListener(topics = topic, groupId = "${spring.kafka.consumer.group-id}")
	@Transactional
	public void listen(JsonNode payload) throws Exception {

		logger.info("inside process of therapist-appointment-events..");

		String eventType = payload.get("eventType").asText();
		AppointmentEvent appointmentEvent = objectMapper.convertValue(payload, AppointmentEvent.class);

		switch (eventType) {

		case "AppointmentConfirmed" -> createInvite(appointmentEvent);
		case "AppointmentRescheduled" -> rescheduleInvite(appointmentEvent);
		case "AppointmentCancelled" -> cancelInvite(appointmentEvent);
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

	private void createInvite(AppointmentEvent appointmentEvent) throws Exception {

		Optional<AppointmentCalendarEvent> existingCalendarEvent = appointmentCalendarEventRepository.findById(appointmentEvent.getAppointmentId());

		if (existingCalendarEvent.isPresent()) {
			logger.info("Calendar invite already exists for appointmentId={}", appointmentEvent.getAppointmentId());
			return;
		}

		ClientProjection clientProjection = clientProjectionRepository.findById(appointmentEvent.getClientId())
				.orElseThrow(() -> new IllegalStateException(
						"Client projection not found for clientId=" + appointmentEvent.getClientId()));

		ZoneId zone = resolveZone(appointmentEvent.getTherapistId());

		String title = "Therapy Session with " + clientProjection.getFirstName() + " " + clientProjection.getLastName();

		String googleCalendarEventId = googleCalendarService.createAppointmentEvent(
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
		appointmentCalendarEvent.setGoogleCalendarEventId(googleCalendarEventId);
		appointmentCalendarEventRepository.save(appointmentCalendarEvent);
	}

	private void rescheduleInvite(AppointmentEvent appointmentEvent) throws Exception {

		Optional<AppointmentCalendarEvent> existingCalendarEvent = appointmentCalendarEventRepository.findById(appointmentEvent.getAppointmentId());

		if (existingCalendarEvent.isEmpty()) {
			logger.info("No existing invite for appointmentId={}; skipping reschedule update", appointmentEvent.getAppointmentId());
			return;
		}

		ClientProjection clientProjection = clientProjectionRepository.findById(appointmentEvent.getClientId())
				.orElseThrow(() -> new IllegalStateException(
						"Client projection not found for clientId=" + appointmentEvent.getClientId()));

		ZoneId zone = resolveZone(appointmentEvent.getTherapistId());

		String title = "Therapy Session with " + clientProjection.getFirstName() + " " + clientProjection.getLastName();

		googleCalendarService.updateAppointmentEvent(
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
	}

	private void cancelInvite(AppointmentEvent appointmentEvent) throws Exception {

		Optional<AppointmentCalendarEvent> mapping = appointmentCalendarEventRepository.findById(appointmentEvent.getAppointmentId());
		if (mapping.isEmpty()) {
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
	}
}
