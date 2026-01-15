package com.org.userService;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;

@EnableDiscoveryClient
@SpringBootApplication
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

	@Bean
	NewTopic loginSuccessTopic() {
		return TopicBuilder.name("auth-login-success").partitions(3).replicas(1).build();
	}
	
	@Bean
	NewTopic loginFailureTopic() {
		return TopicBuilder.name("auth-login-failure").partitions(3).replicas(1).build();
	}
}
