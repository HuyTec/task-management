package com.taskmanagement.service.cache;

import com.taskmanagement.dto.task.TaskDetailResponse;

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
public class TaskCacheService {

    private static final String KEY_PREFIX = "task:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, TaskDetailResponse> redisTemplate;


    private String buildKey(Long userId, Long taskId) {
        return KEY_PREFIX + userId + ":" + taskId;
    }

    public Optional<TaskDetailResponse> get(Long userId, Long taskId) {
        try {
            TaskDetailResponse value = redisTemplate.opsForValue().get(buildKey(userId, taskId));
            return Optional.ofNullable(value);
        } catch (DataAccessException | SerializationException ex) {
            log.warn("Unable to read task {} from cache", taskId, ex);
            return Optional.empty();
        }
    }

    public void put(Long userId, TaskDetailResponse task) {
        try {
            redisTemplate.opsForValue().set(buildKey(userId, task.id()), task, TTL);
        } catch (DataAccessException | SerializationException ex) {
            log.warn("Unable to cache task {}", task.id(), ex);
        }
    }

    public void evict(Long userId, Long taskId) {
        try {
            redisTemplate.delete(buildKey(userId, taskId));
        } catch (DataAccessException ex) {
            log.warn("Unable to evict task {} from cache", taskId, ex);
        }
    }
}
