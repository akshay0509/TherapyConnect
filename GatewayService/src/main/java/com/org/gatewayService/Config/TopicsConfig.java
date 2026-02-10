package com.org.gatewayService.Config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicsConfig {

	@Bean
	NewTopic loginSuccessTopic() {
		return TopicBuilder.name("auth-login-success").partitions(3).replicas(1).build();
	}
	
	@Bean
	NewTopic loginFailureTopic() {
		return TopicBuilder.name("auth-login-failure").partitions(3).replicas(1).build();
	}
	
	@Bean
	NewTopic emailReminderTopic() {
		return TopicBuilder.name("email-reminder-topic").partitions(3).replicas(1).build();
	}
	
	@Bean
	NewTopic therapistAvailabilityTopic() {
		return TopicBuilder.name("therapist-availability-events").partitions(3).replicas(1).build();
	}
	
	@Bean
	NewTopic appointmentTopic() {
		return TopicBuilder.name("therapist-appointment-events").partitions(3).replicas(1).build();
	}
}
