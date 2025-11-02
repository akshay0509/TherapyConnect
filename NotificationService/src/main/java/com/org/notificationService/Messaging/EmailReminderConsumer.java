package com.org.notificationService.Messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.notificationService.Dto.ClientReminderDto;
import com.org.notificationService.Services.EmailSenderService;

@Component
public class EmailReminderConsumer {
	
	@Autowired
	EmailSenderService emailSenderService;
	
	private static final String topic = "email-reminder-topic";
	
	@KafkaListener(topics = topic, groupId = "${spring.kafka.consumer.group-id}")
	public void listen(String message) {
		try {
			ClientReminderDto clientReminderDto = new ObjectMapper().readValue(message, ClientReminderDto.class);
			emailSenderService.sendReminderEmail(clientReminderDto);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

}
