package com.org.appointmentService.Messaging.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.appointmentService.Entity.TherapistPaymentProjection;
import com.org.appointmentService.Repository.TherapistPaymentProjectionRepository;
import com.org.events.Therapist.TherapistEvent;

import jakarta.transaction.Transactional;

/**
 * Maintains the paymentEnabled projection from TherapistService events.
 * Separate consumer group from the slot projection so the two never block
 * each other.
 */
@Component
public class TherapistPaymentSettingsConsumer {

	@Autowired
	private TherapistPaymentProjectionRepository therapistPaymentProjectionRepository;

	@Autowired
	private ObjectMapper objectMapper;

	private static final String topic = "therapist-availability-events";

	private static final Logger logger = LoggerFactory.getLogger(TherapistPaymentSettingsConsumer.class);

	@KafkaListener(topics = topic, groupId = "appointment-therapist-payment-projection-group")
	@Transactional
	public void process(JsonNode payload) {

		String eventType = payload.get("eventType").asText();

		switch (eventType) {
			case "TherapistCreated", "TherapistPaymentSettingsUpdated" ->
					upsertProjection(objectMapper.convertValue(payload, TherapistEvent.class));
			default -> logger.debug("Skipping eventType={} for payment projection", eventType);
		}
	}

	private void upsertProjection(TherapistEvent event) {

		TherapistPaymentProjection projection = therapistPaymentProjectionRepository
				.findById(event.getTherapistId())
				.orElse(new TherapistPaymentProjection());

		projection.setTherapistId(event.getTherapistId());
		projection.setPaymentEnabled(Boolean.TRUE.equals(event.getPaymentEnabled()));

		therapistPaymentProjectionRepository.save(projection);

		logger.info("Upserted TherapistPaymentProjection therapistId={} paymentEnabled={}",
				event.getTherapistId(), event.getPaymentEnabled());
	}
}
