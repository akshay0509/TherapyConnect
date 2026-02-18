package com.org.gatewayService.Utility;


import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtUtil {

	private final RSAPrivateKey privateKey;

	public JwtUtil(RSAPrivateKey privateKey) {
		this.privateKey = privateKey;
	}

	public String generateToken(String username, List<String> scopes, Set<String> authorities, String userId) {
		return Jwts.builder()
				.setIssuer("gateway-auth")
				.setSubject(username)
				.setAudience("therapist-service")
				.claim("scope", scopes)
				.claim("authorities", authorities)
				.claim("userId", userId)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000))
				.signWith(privateKey, SignatureAlgorithm.RS256)
				.compact();
	}
}
