package com.org.gatewayService.Services;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.org.gatewayService.Entity.RefreshTokens;
import com.org.gatewayService.Repository.RefreshTokensRepository;

@Service
public class RefreshTokensService {

	@Autowired
	private RefreshTokensRepository refreshTokensRepository;

	private final long refreshTokenDurationDays = 7;

	@Transactional
	public RefreshTokens createRefreshToken(String username, String userId, String therapistId, Set<String> roles) {
		refreshTokensRepository.deleteByUsername(username);

		RefreshTokens token = new RefreshTokens();
		token.setId(UUID.randomUUID().toString());
		token.setToken(UUID.randomUUID().toString());
		token.setUsername(username);
		token.setUserId(userId);
		token.setTherapistId(therapistId);
		token.setRoles(roles != null ? String.join(",", roles) : "");
		token.setExpiryDate(LocalDate.now().plusDays(refreshTokenDurationDays));
		token.setRevoked(false);

		return refreshTokensRepository.save(token);
	}

	public Set<String> getRoles(RefreshTokens token) {
		if (token.getRoles() == null || token.getRoles().isBlank()) return Set.of();
		return new HashSet<>(Arrays.asList(token.getRoles().split(",")));
	}

	public Optional<RefreshTokens> findByToken(String token) {
		return refreshTokensRepository.findByToken(token);
	}

	public boolean isExpiredOrRevoked(RefreshTokens token) {
		return token.isRevoked() || token.getExpiryDate().isBefore(LocalDate.now());
	}

	@Transactional
	public void revokeByUsername(String username) {
		refreshTokensRepository.deleteByUsername(username);
	}
}
