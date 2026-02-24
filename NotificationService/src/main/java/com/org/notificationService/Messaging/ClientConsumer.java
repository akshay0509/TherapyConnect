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
		case "ClientCreated" -> {
			ClientEvent clientEvent = objectMapper.convertValue(payload, ClientEvent.class);
			createClient(clientEvent);
		}
		}
	}
	
	private void createClient(ClientEvent event) {
		
		ClientProjection clientProjection = new ClientProjection();
		clientProjection.setClientId(event.getClientId());
		clientProjection.setFirstName(event.getFirstName());
		clientProjection.setLastName(event.getLastName());
		clientProjection.setEmail(event.getEmail());
		clientProjection.setCreatedAt(event.getOccurredAt());
		
		clientProjectionRepository.save(clientProjection);
	}
}
