package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import com.taskmanagement.service.auth.JwtService;

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
        String token = jwtService.generateToken(user.getUsername());

        assertThat(jwtService.isAccessToken(token)).isTrue();
        assertThat(jwtService.isRefreshToken(token)).isFalse();
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void refreshTokenCannotAuthenticateApiRequests() {
        String token = jwtService.generateRefreshToken(user.getUsername());

        assertThat(jwtService.isRefreshToken(token)).isTrue();
        assertThat(jwtService.isAccessToken(token)).isFalse();
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }
}
