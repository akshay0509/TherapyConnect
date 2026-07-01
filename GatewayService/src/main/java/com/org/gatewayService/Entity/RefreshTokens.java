package com.org.gatewayService.Entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "REFRESH_TOKENS")
public class RefreshTokens {

	@Id
    private String id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private boolean revoked;

    @Column
    private String userId;

    @Column
    private String therapistId;

    // Comma-separated role names stored so refresh can reissue a JWT with the same authorities
    @Column
    private String roles;
}
