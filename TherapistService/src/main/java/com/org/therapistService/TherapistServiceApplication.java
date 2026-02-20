package com.org.therapistService;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import com.org.therapistService.Config.JwtPropagationInterceptor;
import com.org.therapistService.Config.TraceIdInterceptor;

@EnableDiscoveryClient
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class TherapistServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TherapistServiceApplication.class, args);
	}
	
	@Bean
	@LoadBalanced
	RestTemplate restTemmplate() { 
		RestTemplate restTemplate = new RestTemplate();
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		interceptors.add(new TraceIdInterceptor());
		interceptors.add(new JwtPropagationInterceptor());
		restTemplate.setInterceptors(interceptors);
		return restTemplate;
	}
	
}
