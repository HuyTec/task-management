package com.taskmanagement.service.auth;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.taskmanagement.exception.AuthenticationStoreUnavailableException;

@Service
public class AuthSessionService {

    private static final String KEY_PREFIX = "auth:refresh-session:";

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if not current or current ~= ARGV[1] then
                return 0
            end
            redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration sessionTtl;

    public AuthSessionService(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.refresh-expiration}") long refreshExpiration
    ) {
        if (refreshExpiration <= 0) {
            throw new IllegalArgumentException("JWT refresh expiration must be positive");
        }
        this.redisTemplate = redisTemplate;
        this.sessionTtl = Duration.ofMillis(refreshExpiration);
    }

    public void create(String sessionId, String tokenId) {
        try {
            redisTemplate.opsForValue().set(buildKey(sessionId), tokenId, sessionTtl);
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        }
    }

    public boolean rotate(String sessionId, String expectedTokenId, String newTokenId) {
        try {
            Long result = redisTemplate.execute(
                    ROTATE_SCRIPT,
                    List.of(buildKey(sessionId)),
                    expectedTokenId,
                    newTokenId,
                    Long.toString(sessionTtl.toMillis())
            );
            return Long.valueOf(1L).equals(result);
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        }
    }

    public void revoke(String sessionId) {
        try {
            redisTemplate.delete(buildKey(sessionId));
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        }
    }

    private String buildKey(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    public boolean exists(String sessionId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(sessionId)));
        } catch (DataAccessException ex) {
            throw unavailable(ex);
        }
    }

    private AuthenticationStoreUnavailableException unavailable(DataAccessException cause) {
        return new AuthenticationStoreUnavailableException(
                "Refresh session store is unavailable",
                cause
        );
    }
}
