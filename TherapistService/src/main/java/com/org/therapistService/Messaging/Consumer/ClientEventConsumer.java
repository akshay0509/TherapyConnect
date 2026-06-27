package com.org.therapistService.Messaging.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.events.Client.ClientEvent;
import com.org.therapistService.Entity.TherapistClients;
import com.org.therapistService.Repository.TherapistClientsRepository;

import jakarta.transaction.Transactional;

@Service
public class ClientEventConsumer {

	@Autowired
	private TherapistClientsRepository therapistClientsRepository;

	@Autowired
	private ObjectMapper objectMapper;

	private static final String TOPIC = "client-events";
	private static final Logger logger = LoggerFactory.getLogger(ClientEventConsumer.class);

	@KafkaListener(topics = TOPIC, groupId = "${spring.kafka.consumer.group-id}")
	@Transactional
	public void listen(JsonNode payload) {
		String eventType = payload.get("eventType").asText();

		if (!"ClientUpdated".equals(eventType) && !"ClientStatusUpdated".equals(eventType)) {
			return;
		}

		ClientEvent event = objectMapper.convertValue(payload, ClientEvent.class);
		TherapistClients therapistClient = therapistClientsRepository
				.findByTherapistIdAndClientId(event.getTherapistId(), event.getClientId())
				.orElse(null);

		if (therapistClient == null) {
			logger.warn("Therapist client projection missing for client event. therapistId={}, clientId={}, eventType={}",
					event.getTherapistId(), event.getClientId(), eventType);
			return;
		}

		if ("ClientUpdated".equals(eventType)) {
			therapistClient.setClientName((event.getFirstName() + " " + event.getLastName()).trim());
		}

		if (event.getStatus() != null) {
			therapistClient.setStatus(event.getStatus());
		}

		therapistClientsRepository.save(therapistClient);
	}
}
