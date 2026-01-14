package com.org.gatewayService.Config;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtDecoderConfig {
	
	@Value("${jwt.secret}")
	private String secretKey;
	
	@Bean
	public JwtDecoder jwtDecoder() {
		
		byte[] secretBytes = java.util.Base64.getDecoder().decode(secretKey);
		return NimbusJwtDecoder.withSecretKey(
				new SecretKeySpec(secretBytes, "HmacSHA256")
		).build();
	}
}
