package com.org.therapistService;

import java.util.Collections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import com.org.therapistService.Config.TraceIdInterceptor;

@EnableDiscoveryClient
@SpringBootApplication
@EnableScheduling
public class TherapistServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TherapistServiceApplication.class, args);
	}
	
	@Bean
	@LoadBalanced
	RestTemplate restTemmplate() { 
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setInterceptors(Collections.singletonList(new TraceIdInterceptor()));
		return restTemplate;
	}
	
}
