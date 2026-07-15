package com.taskmanagement.service.cache;

import com.taskmanagement.dto.task.TaskResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class TaskCacheService {

    private static final String KEY_PREFIX = "task:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    public TaskCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildKey(Long taskId) {
        return KEY_PREFIX + taskId;
    }

    public Optional<TaskResponse> get(Long taskId) {
        try {
            Object value = redisTemplate.opsForValue().get(buildKey(taskId));

            if (value instanceof TaskResponse task) {
                return Optional.of(task);
            }

            return Optional.empty();

        } catch (SerializationException ex) {
            // Cache chứa dữ liệu cũ/hỏng không deserialize được -> coi như cache-miss, dọn key hỏng
            redisTemplate.delete(buildKey(taskId));
            return Optional.empty();
        }
    }

    public void put(TaskResponse task) {
        redisTemplate.opsForValue().set(buildKey(task.id()),task,TTL);
    }

    public void evict(Long taskId) {
        redisTemplate.delete(buildKey(taskId));
    }

    // public boolean exists(Long taskId) {
    //     // TODO: Check cache existence
    //     return false;
    // }

    // public void clear() {
    //     // TODO: (Optional) Clear all task cache
    // }
}