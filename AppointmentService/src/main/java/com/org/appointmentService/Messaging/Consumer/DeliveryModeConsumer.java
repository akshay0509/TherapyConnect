package com.org.appointmentService.Messaging.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.appointmentService.Entity.TherapyDeliveryMode;
import com.org.appointmentService.Enums.DeliveryModeType;
import com.org.appointmentService.Repository.TherapyDeliveryModeRepository;
import com.org.events.TherapistAvailability.DeliveryModeEvent;

import jakarta.transaction.Transactional;

@Component
public class DeliveryModeConsumer {

	@Autowired
	private TherapyDeliveryModeRepository therapyDeliveryModeRepository;

	@Autowired
	private ObjectMapper objectMapper;

	private static final String topic = "therapist-availability-events";

	private static final Logger logger = LoggerFactory.getLogger(DeliveryModeConsumer.class);

	@KafkaListener(topics = topic, groupId = "appointment-delivery-mode-projection-group")
	@Transactional
	public void process(JsonNode payload) {
		logger.debug("inside process of DeliveryModeConsumer..");

		String eventType = payload.get("eventType").asText();

		switch (eventType) {

		case "DeliveryModeCreated", "DeliveryModeUpdated" -> {
			DeliveryModeEvent event = objectMapper.convertValue(payload, DeliveryModeEvent.class);
			createOrUpdateMode(event);
		}

		case "DeliveryModeDeleted" -> {
			DeliveryModeEvent event = objectMapper.convertValue(payload, DeliveryModeEvent.class);
			deleteMode(event);
		}

		}
	}

	private void createOrUpdateMode(DeliveryModeEvent event) {
		TherapyDeliveryMode mode = therapyDeliveryModeRepository.findById(event.getModeId()).orElse(new TherapyDeliveryMode());
		mode.setModeId(event.getModeId());
		mode.setTherapistId(event.getTherapistId());
		mode.setServiceId(event.getServiceId());
		mode.setModeType(DeliveryModeType.valueOf(event.getModeType()));
		mode.setDisplayName(event.getDisplayName());
		mode.setAddress(event.getAddress());
		mode.setPrice(event.getPrice());
		mode.setActive(Boolean.TRUE.equals(event.getIsActive()));
		therapyDeliveryModeRepository.save(mode);

		logger.info("Projected delivery mode. modeId={}, eventType={}", event.getModeId(), event.getEventType());
	}

	private void deleteMode(DeliveryModeEvent event) {
		therapyDeliveryModeRepository.deleteById(event.getModeId());
		logger.info("Removed delivery mode projection. modeId={}", event.getModeId());
	}
}
