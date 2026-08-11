package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import com.taskmanagement.exception.AuthenticationStoreUnavailableException;
import com.taskmanagement.service.auth.AuthSessionService;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

    private static final long TTL_MILLIS = 120_000L;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private AuthSessionService authSessionService;

    @BeforeEach
    void setUp() {
        authSessionService = new AuthSessionService(redisTemplate, TTL_MILLIS);
    }

    @Test
    void createStoresCurrentTokenIdWithRefreshTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authSessionService.create("session-1", "token-1");

        verify(valueOperations).set(
                "auth:refresh-session:session-1",
                "token-1",
                Duration.ofMillis(TTL_MILLIS)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void rotateReturnsTrueOnlyWhenRedisReplacesExpectedToken() {
        when(redisTemplate.execute(
                any(RedisScript.class),
                eq(List.of("auth:refresh-session:session-1")),
                eq("token-1"),
                eq("token-2"),
                eq(Long.toString(TTL_MILLIS))
        )).thenReturn(1L);

        boolean rotated = authSessionService.rotate("session-1", "token-1", "token-2");

        assertThat(rotated).isTrue();
    }

    @Test
    void createFailsClosedWhenRedisIsUnavailable() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RedisConnectionFailureException("Redis unavailable"))
                .when(valueOperations)
                .set(
                        "auth:refresh-session:session-1",
                        "token-1",
                        Duration.ofMillis(TTL_MILLIS)
                );

        assertThatThrownBy(() -> authSessionService.create("session-1", "token-1"))
                .isInstanceOf(AuthenticationStoreUnavailableException.class)
                .hasMessage("Refresh session store is unavailable");
    }
}
