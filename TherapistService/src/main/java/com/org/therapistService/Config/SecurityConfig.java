package com.org.therapistService.Config;

import jakarta.servlet.DispatcherType;
import static org.springframework.security.config.Customizer.withDefaults;

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
				/* When a handler throws, Boot FORWARDS to /error, and that forward
				   re-enters this filter chain. On a permitAll endpoint the forwarded
				   request carries no authentication, so it is denied and the caller
				   gets an opaque 401 in place of the real status — a 400 for a bad
				   payload and a 500 for a genuine fault come back identical and
				   undebuggable from outside. Diagnosed the hard way on the Google
				   Forms webhook (ClientService).

				   This does not widen access: the original request was already
				   authorized before the handler ran. */
				.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
				.requestMatchers("/internal/**").permitAll()
				.anyRequest().authenticated()
			)
			.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(withDefaults())
			);
			
		return http.build();
	}
}
