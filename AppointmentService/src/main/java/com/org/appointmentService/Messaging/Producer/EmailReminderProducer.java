package com.org.appointmentService.Messaging.Producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.org.events.email.EmailReminderEvent;


@Service
public class EmailReminderProducer {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;
	
	private static final String topic = "email-reminder-events";
	
	public void sendMessage(String clientId, EmailReminderEvent emailReminderEvent) {
		
		kafkaTemplate.send(topic, clientId, emailReminderEvent);
	}
}
