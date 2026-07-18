package com.taskmanagement.service.cache;

import com.taskmanagement.dto.expense.ExpenseResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpenseCacheService {

    private static final String KEY_PREFIX = "expense:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, ExpenseResponse> redisTemplate;

    private String buildKey(Long taskId) {
        return KEY_PREFIX + taskId;
    }

    public Optional<ExpenseResponse> get(Long taskId) {
        try {
            ExpenseResponse value = redisTemplate.opsForValue().get(buildKey(taskId));
            return Optional.ofNullable(value);

        } catch (SerializationException ex) {
            redisTemplate.delete(buildKey(taskId));
            return Optional.empty();
        }
    }

    public void put(ExpenseResponse expense) {
        redisTemplate.opsForValue().set(buildKey(expense.id()),expense,TTL);
    }

    public void evict(Long expenseId) {
        redisTemplate.delete(buildKey(expenseId));
    }
}