package com.taskmanagement.service.cache;

import com.taskmanagement.dto.expense.ExpenseResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseCacheService {

    private static final String KEY_PREFIX = "expense:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, ExpenseResponse> redisTemplate;

    private String buildKey(Long userId, Long expenseId) {
        return KEY_PREFIX + userId + ":" + expenseId;
    }

    public Optional<ExpenseResponse> get(Long userId, Long expenseId) {
        try {
            ExpenseResponse value = redisTemplate.opsForValue().get(buildKey(userId, expenseId));
            return Optional.ofNullable(value);
        } catch (DataAccessException | SerializationException ex) {
            log.warn("Unable to read expense {} from cache", expenseId, ex);
            return Optional.empty();
        }
    }

    public void put(Long userId, ExpenseResponse expense) {
        try {
            redisTemplate.opsForValue().set(buildKey(userId, expense.id()), expense, TTL);
        } catch (DataAccessException | SerializationException ex) {
            log.warn("Unable to cache expense {}", expense.id(), ex);
        }
    }

    public void evict(Long userId, Long expenseId) {
        try {
            redisTemplate.delete(buildKey(userId, expenseId));
        } catch (DataAccessException ex) {
            log.warn("Unable to evict expense {} from cache", expenseId, ex);
        }
    }
}
