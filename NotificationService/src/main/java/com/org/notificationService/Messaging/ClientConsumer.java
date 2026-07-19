package com.org.notificationService.Messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.events.Client.ClientEvent;
import com.org.notificationService.Entity.ClientProjection;
import com.org.notificationService.Repository.ClientProjectionRepository;

@Service
public class ClientConsumer {

	@Autowired
	ObjectMapper objectMapper;
	
	@Autowired
	private ClientProjectionRepository clientProjectionRepository;

	private static final String topic = "client-events";

	@KafkaListener(topics = topic, groupId = "${spring.kafka.consumer.group-id}")
	@Transactional
	public void process(JsonNode payload) {

		String eventType = payload.get("eventType").asText();

		switch (eventType) {
		// ClientUpdated must upsert too: this projection supplies the email
		// calendar invites are sent to, so a contact-detail edit has to reach
		// it — previously only ClientCreated was consumed and edits were lost
		case "ClientCreated", "ClientUpdated" -> {
			ClientEvent clientEvent = objectMapper.convertValue(payload, ClientEvent.class);
			upsertClient(clientEvent);
		}
		}
	}

	private void upsertClient(ClientEvent event) {

		ClientProjection clientProjection = clientProjectionRepository.findById(event.getClientId())
				.orElseGet(() -> {
					ClientProjection created = new ClientProjection();
					created.setClientId(event.getClientId());
					created.setCreatedAt(event.getOccurredAt());
					return created;
				});
		clientProjection.setFirstName(event.getFirstName());
		clientProjection.setLastName(event.getLastName());
		clientProjection.setEmail(event.getEmail());
		clientProjection.setUpdatedAt(event.getOccurredAt());

		clientProjectionRepository.save(clientProjection);
	}
}
