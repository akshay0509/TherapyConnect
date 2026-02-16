package com.org.gatewayService.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.org.gatewayService.Entity.RefreshTokens;

@Repository
public interface RefreshTokensRepository extends JpaRepository<String, RefreshTokens>{

	Optional<RefreshTokens> findByToken(String token);

    void deleteByUsername(String username);
}
