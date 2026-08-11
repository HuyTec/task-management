package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import com.taskmanagement.dto.auth.RefreshTokenClaims;
import com.taskmanagement.service.auth.JwtService;

import io.jsonwebtoken.MalformedJwtException;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();
    private final UserDetails user = User.withUsername("alice")
            .password("unused")
            .roles("USER")
            .build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "test-secret-that-is-long-enough-for-hmac-signing");
        ReflectionTestUtils.setField(jwtService, "expiration", 60_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 120_000L);
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshToken() {
        String sessionId = UUID.randomUUID().toString();
        String token = jwtService.generateAccessToken(user.getUsername(), sessionId);

        assertThat(jwtService.isAccessToken(token)).isTrue();
        assertThat(jwtService.isRefreshToken(token)).isFalse();
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
        assertThat(jwtService.extractSessionId(token)).isEqualTo(sessionId);
    }

    @Test
    void refreshTokenCannotAuthenticateApiRequests() {
        String sessionId = UUID.randomUUID().toString();
        String tokenId = UUID.randomUUID().toString();
        String token = jwtService.generateRefreshToken(user.getUsername(), sessionId, tokenId);

        assertThat(jwtService.isRefreshToken(token)).isTrue();
        assertThat(jwtService.isAccessToken(token)).isFalse();
        assertThat(jwtService.isTokenValid(token, user)).isFalse();

        RefreshTokenClaims claims = jwtService.parseRefreshToken(token);
        assertThat(claims.username()).isEqualTo(user.getUsername());
        assertThat(claims.sessionId()).isEqualTo(sessionId);
        assertThat(claims.tokenId()).isEqualTo(tokenId);
    }

    @Test
    void accessTokenCannotBeParsedAsRefreshToken() {
        String sessionId = UUID.randomUUID().toString();
        String accessToken = jwtService.generateAccessToken(user.getUsername(), sessionId);

        assertThatThrownBy(() -> jwtService.parseRefreshToken(accessToken))
                .isInstanceOf(MalformedJwtException.class)
                .hasMessage("Token is not a refresh token");
    }
}
