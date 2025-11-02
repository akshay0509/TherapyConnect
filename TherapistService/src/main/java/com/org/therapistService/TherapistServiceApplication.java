package com.org.therapistService;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.web.client.RestTemplate;

@EnableDiscoveryClient
@SpringBootApplication
public class TherapistServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TherapistServiceApplication.class, args);
	}
	
	@Bean
	@LoadBalanced
	RestTemplate restTemmplate() { 
		return new RestTemplate();
	}
	
	@Bean
	NewTopic myTopic() {
		return TopicBuilder.name("email-reminder-topic").partitions(3).replicas(1).build();
	}

}
