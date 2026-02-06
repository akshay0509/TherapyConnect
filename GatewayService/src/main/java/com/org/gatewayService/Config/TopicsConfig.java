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
	NewTopic myTopic() {
		return TopicBuilder.name("email-reminder-topic").partitions(3).replicas(1).build();
	}
}
