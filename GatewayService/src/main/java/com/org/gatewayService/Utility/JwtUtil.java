package com.org.gatewayService.Utility;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JwtUtil {
	
	@Value("${jwt.secret}")
	private static String secretKey;

	public static String generateToken(String username, List<String> scopes, List<String> authorities) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("scope", scopes);
		claims.put("authorities", authorities);
        return Jwts.builder()
        		.setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000)) // 60 mins
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }
}
