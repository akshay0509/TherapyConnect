package com.org.therapistService.Messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class OutboxEventProducer {

	@Autowired
	private KafkaTemplate<String, JsonNode> kafkaTemplate;

	private static final String topic = "therapist-availability-events";
	
	public void sendMessage(String therapistId, JsonNode payload) {

		try {
			kafkaTemplate.send(topic, therapistId, payload).get();
		}
		catch (Exception e) {
		    throw new RuntimeException("Kafka publish failed", e);
		}
	}
}
