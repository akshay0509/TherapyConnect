package com.org.therapistService.Messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.events.email.EmailReminderEvent;


@Service
public class EmailReminderProducer {

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;
	
	private static final String topic = "email-reminder-topic";
	
	public void sendMessage(String clientId, EmailReminderEvent emailReminderEvent) {
		String jsonMessage;
		try {
			jsonMessage = new ObjectMapper().writeValueAsString(emailReminderEvent);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			jsonMessage = null;
		}
		kafkaTemplate.send(topic, clientId, jsonMessage);
	}
}
