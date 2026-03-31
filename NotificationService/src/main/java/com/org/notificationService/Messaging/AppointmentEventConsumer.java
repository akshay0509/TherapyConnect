package com.org.notificationService.Messaging;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.events.TherapistAppointment.AppointmentEvent;
import com.org.notificationService.Entity.AppointmentCalendarEvent;
import com.org.notificationService.Entity.ClientProjection;
import com.org.notificationService.Repository.AppointmentCalendarEventRepository;
import com.org.notificationService.Repository.ClientProjectionRepository;
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
	AppointmentCalendarEventRepository appointmentCalendarEventRepository;


	private static final String topic = "therapist-appointment-events";
	private static final String therapistEmail = "appintegrationtesting2@gmail.com";

	private static final Logger logger = LoggerFactory.getLogger(AppointmentEventConsumer.class);

	@KafkaListener(topics = topic, groupId = "${spring.kafka.consumer.group-id}")
	@Transactional
	public void listen(JsonNode payload) {

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

	private void createInvite(AppointmentEvent appointmentEvent) {

		Optional<AppointmentCalendarEvent> existingCalendarEvent = appointmentCalendarEventRepository.findById(appointmentEvent.getAppointmentId());

		if (existingCalendarEvent.isPresent()) {
			logger.info("Calendar invite already exists for appointmentId={}", appointmentEvent.getAppointmentId());
			return;
		}

		Optional<ClientProjection> clientProjection = clientProjectionRepository.findById(appointmentEvent.getClientId());

		if (clientProjection.isEmpty()) {
			logger.error("Client not found. Skipping event. clientId={}", appointmentEvent.getClientId());
			return;
		}

		try {

			String googleCalendarEventId = googleCalendarService.createAppointmentEvent(
					clientProjection.get().getEmail(),
					therapistEmail,
					"Therapy Session",
					"Appointment ID: " + appointmentEvent.getAppointmentId(),
					appointmentEvent.getStartTime(),
					appointmentEvent.getEndTime()
					);

			AppointmentCalendarEvent appointmentCalendarEvent = new AppointmentCalendarEvent();
			appointmentCalendarEvent.setAppointmentId(appointmentEvent.getAppointmentId());
			appointmentCalendarEvent.setGoogleCalendarEventId(googleCalendarEventId);
			appointmentCalendarEventRepository.save(appointmentCalendarEvent);
		}
		catch (Exception e) {
			logger.error("Failed to create calendar event for appointmentId={}", appointmentEvent.getAppointmentId(), e);
		}
	}

	private void rescheduleInvite(AppointmentEvent appointmentEvent) {

		Optional<AppointmentCalendarEvent> existingCalendarEvent = appointmentCalendarEventRepository.findById(appointmentEvent.getAppointmentId());

		if (existingCalendarEvent.isEmpty()) {
			logger.info("No existing invite for appointmentId={}; skipping reschedule update", appointmentEvent.getAppointmentId());
			return;
		}

		Optional<ClientProjection> clientProjection = clientProjectionRepository.findById(appointmentEvent.getClientId());
		
		if (clientProjection.isEmpty()) {
			logger.error("Client not found. Skipping reschedule event. clientId={}", appointmentEvent.getClientId());
			return;
		}

		try {
			googleCalendarService.updateAppointmentEvent(
					existingCalendarEvent.get().getGoogleCalendarEventId(),
					clientProjection.get().getEmail(),
					therapistEmail,
					"Therapy Session",
					"Appointment ID: " + appointmentEvent.getAppointmentId(),
					appointmentEvent.getStartTime(),
					appointmentEvent.getEndTime()
					);
		}
		catch (Exception e) {
			logger.error("Failed to update calendar event for appointmentId={}", appointmentEvent.getAppointmentId(), e);
		}
	}

	private void cancelInvite(AppointmentEvent appointmentEvent) {

		Optional<AppointmentCalendarEvent> mapping = appointmentCalendarEventRepository.findById(appointmentEvent.getAppointmentId());
		if (mapping.isEmpty()) {
			logger.warn("Calendar mapping not found for cancel. appointmentId={}", appointmentEvent.getAppointmentId());
			return;
		}

		try {
			googleCalendarService.cancelAppointmentEvent(mapping.get().getGoogleCalendarEventId());
			appointmentCalendarEventRepository.delete(mapping.get());
		}
		catch (Exception e) {
			logger.error("Failed to cancel calendar event for appointmentId={}", appointmentEvent.getAppointmentId(), e);
		}
	}
}
