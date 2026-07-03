package com.org.appointmentService.Messaging.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.appointmentService.Entity.ClientContactProjection;
import com.org.appointmentService.Repository.ClientContactProjectionRepository;
import com.org.events.Client.ClientEvent;

import jakarta.transaction.Transactional;

/**
 * Maintains client contact details (email/phone) so payment links can be
 * delivered to the client via Razorpay's SMS/email notify.
 */
@Component
public class ClientContactConsumer {

	@Autowired
	private ClientContactProjectionRepository clientContactProjectionRepository;

	@Autowired
	private ObjectMapper objectMapper;

	private static final String topic = "client-events";

	private static final Logger logger = LoggerFactory.getLogger(ClientContactConsumer.class);

	@KafkaListener(topics = topic, groupId = "appointment-client-contact-projection-group")
	@Transactional
	public void process(JsonNode payload) {

		String eventType = payload.get("eventType").asText();

		switch (eventType) {
			case "ClientCreated", "ClientUpdated" ->
					upsertProjection(objectMapper.convertValue(payload, ClientEvent.class));
			default -> logger.debug("Skipping eventType={} for client contact projection", eventType);
		}
	}

	private void upsertProjection(ClientEvent event) {

		ClientContactProjection projection = clientContactProjectionRepository
				.findById(event.getClientId())
				.orElse(new ClientContactProjection());

		projection.setClientId(event.getClientId());
		projection.setFirstName(event.getFirstName());
		projection.setLastName(event.getLastName());
		projection.setEmail(event.getEmail());
		projection.setPhoneNumber(event.getPhoneNumber());

		clientContactProjectionRepository.save(projection);

		logger.info("Upserted ClientContactProjection clientId={}", event.getClientId());
	}
}
