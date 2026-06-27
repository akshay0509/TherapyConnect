package com.org.userService.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
		http
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(authz -> authz
			.requestMatchers("/**/validate-user").permitAll()
			.requestMatchers("/create-user").permitAll()
			.requestMatchers("/forgot-password", "/reset-password").permitAll()
			);
			
		return http.build();
	}
}
