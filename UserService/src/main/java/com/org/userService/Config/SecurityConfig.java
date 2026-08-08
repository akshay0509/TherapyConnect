package com.org.userService.Config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
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
			.requestMatchers("/**/validate-user").permitAll()
			.requestMatchers("/create-user").permitAll()
			.requestMatchers("/forgot-password", "/reset-password", "/forgot-username").permitAll()
			.requestMatchers("/admin/**").hasAuthority("ADMIN")
				.anyRequest().authenticated()
			)
			.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
			);

		return http.build();
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
		// Extract the "authorities" claim (e.g. ["ROLE_THERAPIST"] or ["ADMIN"]) as-is
		grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
		grantedAuthoritiesConverter.setAuthorityPrefix("");

		JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
		jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
		return jwtConverter;
	}
}
