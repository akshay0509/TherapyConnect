package com.org.gatewayService.Utility;


import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
	
	@Value("${jwt.secret}")
	private String secret;

	public String generateToken(String username, List<String> scopes, List<String> authorities) {
		Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
		System.out.println("Secret key is "+secret);
		Map<String, Object> claims = new HashMap<>();
		claims.put("scope", scopes);
		claims.put("authorities", authorities);
        return Jwts.builder()
        		.setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000)) // 60 mins
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
