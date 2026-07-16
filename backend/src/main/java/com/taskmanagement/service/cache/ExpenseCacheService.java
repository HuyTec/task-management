package com.taskmanagement.service.cache;

import com.taskmanagement.dto.expense.ExpenseResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class ExpenseCacheService {

    private static final String KEY_PREFIX = "expense:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    public ExpenseCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildKey(Long taskId) {
        return KEY_PREFIX + taskId;
    }

    public Optional<ExpenseResponse> get(Long expenseId) {
        try {
            Object value = redisTemplate.opsForValue().get(buildKey(expenseId));

            if (value instanceof ExpenseResponse task) {
                return Optional.of(task);
            }

            return Optional.empty();

        } catch (SerializationException ex) {
            // Cache chứa dữ liệu cũ/hỏng không deserialize được -> coi như cache-miss, dọn key hỏng
            redisTemplate.delete(buildKey(expenseId));
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