package com.org.clientService.Config;

import static org.springframework.security.config.Customizer.withDefaults;

import jakarta.servlet.DispatcherType;

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
				/* When a handler throws, Spring Boot FORWARDS to /error to render the
				   response. That forward is a fresh dispatch and hits this chain
				   again — so without this line the error page itself is denied and
				   the caller gets an opaque 401 instead of the real status.
				   It masked every failure on the public webhook: a 400 for a bad
				   payload and a 401 for a bad signature both came back as an empty
				   401, which is impossible to debug from the outside.
				   Permitting the ERROR dispatch does not widen access — the original
				   request was already authorized before the handler ran. */
				.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
				.requestMatchers("/google-forms/*/submissions").permitAll()
				.anyRequest().authenticated()
			)
			.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(withDefaults())
			);
			
		return http.build();
	}
}
