package com.taskmanagement.service.cache;

import com.taskmanagement.dto.task.TaskDetailResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskCacheService {

    private static final String KEY_PREFIX = "task:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, TaskDetailResponse> redisTemplate;


    private String buildKey(Long taskId) {
        return KEY_PREFIX + taskId;
    }

    public Optional<TaskDetailResponse> get(Long taskId) {
        try {
            TaskDetailResponse value = redisTemplate.opsForValue().get(buildKey(taskId));
            return Optional.ofNullable(value);

        } catch (SerializationException ex) {
            redisTemplate.delete(buildKey(taskId));
            return Optional.empty();
        }
    }

    public void put(TaskDetailResponse task) {
        System.out.println("Put function excuted");
        redisTemplate.opsForValue().set(buildKey(task.id()),task,TTL);
    }

    public void evict(Long taskId) {
         System.out.println("Evict funtion excuted");
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