package com.org.appointmentService.Messaging.Consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.appointmentService.Entity.TherapistServiceProjection;
import com.org.appointmentService.Repository.TherapistServiceProjectionRepository;
import com.org.events.TherapistAvailability.TherapistServiceDefinitionEvent;

import jakarta.transaction.Transactional;

@Component
public class TherapistServiceDefinitionConsumer {

	private final TherapistServiceProjectionRepository repository;
	private final ObjectMapper objectMapper;

	public TherapistServiceDefinitionConsumer(
			TherapistServiceProjectionRepository repository,
			ObjectMapper objectMapper) {
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(
			topics = "therapist-availability-events",
			groupId = "appointment-service-definition-projection-group")
	@Transactional
	public void process(JsonNode payload) {
		String eventType = payload.path("eventType").asText();
		if (!"TherapistServiceDefinitionUpserted".equals(eventType)
				&& !"TherapistServiceDefinitionDeleted".equals(eventType)) {
			return;
		}

		TherapistServiceDefinitionEvent event =
				objectMapper.convertValue(payload, TherapistServiceDefinitionEvent.class);

		if ("TherapistServiceDefinitionDeleted".equals(eventType)) {
			repository.deleteById(event.getServiceId());
			return;
		}

		// Sessions round up to whole 30-minute blocks, so any positive duration
		// is projectable — no multiple-of-30 requirement.
		if (event.getDurationMinutes() == null
				|| event.getDurationMinutes() <= 0
				|| event.getDurationMinutes() > 480) {
			throw new IllegalArgumentException(
					"Projected service duration must be between 1 and 480 minutes.");
		}

		TherapistServiceProjection projection =
				repository.findById(event.getServiceId()).orElseGet(TherapistServiceProjection::new);
		projection.setServiceId(event.getServiceId());
		projection.setTherapistId(event.getTherapistId());
		projection.setDurationMinutes(event.getDurationMinutes());
		projection.setActive(Boolean.TRUE.equals(event.getIsActive()));
		repository.save(projection);
	}
}
