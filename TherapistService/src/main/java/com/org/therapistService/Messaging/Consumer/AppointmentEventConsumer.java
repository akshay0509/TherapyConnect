package com.org.therapistService.Messaging.Consumer;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.events.TherapistAppointment.AppointmentEvent;
import com.org.events.TherapistAppointment.AppointmentStatus;
import com.org.therapistService.Entity.AppointmentProjection;
import com.org.therapistService.Repository.AppointmentProjectionRepository;

import jakarta.transaction.Transactional;

@Service
public class AppointmentEventConsumer {

	@Autowired
	private AppointmentProjectionRepository appointmentProjectionRepository;

	@Autowired
	ObjectMapper objectMapper;

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
			createAppointment(appointmentEvent);
		}
		}
	}

	private void createAppointment(AppointmentEvent event) {

		AppointmentProjection appointmentProjection = new AppointmentProjection();
		appointmentProjection.setAppointmentId(event.getAppointmentId());
		appointmentProjection.setTherapistId(event.getTherapistId());
		appointmentProjection.setClientId(event.getClientId());
		appointmentProjection.setStartTime(event.getStartTime());
		appointmentProjection.setEndTime(event.getEndTime());
		if("AppointmentCreated".equals(event.getEventType()))
			appointmentProjection.setStatus(AppointmentStatus.SCHEDULED);
		appointmentProjection.setSessionType(event.getSessionType());
		appointmentProjection.setUpdatedAt(LocalDateTime.now());

		appointmentProjectionRepository.save(appointmentProjection);
	}
}
