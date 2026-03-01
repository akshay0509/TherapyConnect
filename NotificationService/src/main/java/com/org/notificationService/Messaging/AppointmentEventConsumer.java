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
import com.org.notificationService.Entity.ClientProjection;
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


	private static final String topic = "therapist-appointment-events";

	private static final Logger logger = LoggerFactory.getLogger(AppointmentEventConsumer.class);

	@KafkaListener(topics = topic, groupId = "${spring.kafka.consumer.group-id}")
	@Transactional
	public void listen(JsonNode payload) {

		logger.debug("inside process of therapist-appointment-events..");

		String eventType = payload.get("eventType").asText();

		switch (eventType) {

		case "AppointmentCreated" -> {
			AppointmentEvent appointmentEvent = objectMapper.convertValue(payload, AppointmentEvent.class);

			Optional<ClientProjection> clientProjection = clientProjectionRepository.findById(appointmentEvent.getClientId());

			if (clientProjection.isEmpty()) {
				logger.error("Client not found. Skipping event. clientId={}", appointmentEvent.getClientId());
			    return;
			}

			try {
				googleCalendarService.createAppointmentEvent(
						clientProjection.get().getEmail(),
						"appintegrationtesting2@gmail.com",
						"Therapy Session",
						"Appointment ID: " + appointmentEvent.getAppointmentId(),
						appointmentEvent.getStartTime(),
						appointmentEvent.getEndTime()
						);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		}
	}
}
