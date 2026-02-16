package com.org.gatewayService.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.org.gatewayService.Entity.RefreshTokens;
import com.org.gatewayService.Repository.RefreshTokensRepository;

@Service
public class RefreshTokensService {

	@Autowired
	private RefreshTokensRepository refreshTokensRepository;
	
	private final long refreshTokenDurationSeconds = 7 * 24 * 60 * 60; // 7 days
	
	public RefreshTokens createRefreshToken() {
		
		return new RefreshTokens();
	}
}
