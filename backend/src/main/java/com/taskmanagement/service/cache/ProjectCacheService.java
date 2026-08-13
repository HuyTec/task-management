package com.taskmanagement.service.cache;

import java.time.Duration;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;

import com.taskmanagement.dto.project.ProjectResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectCacheService {

    private static final String KEY_PREFIX = "project:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, ProjectResponse> redisTemplate;

    private String buildKey(Long userId, Long projectId) {
        return KEY_PREFIX + userId + ":" + projectId;
    }

    public Optional<ProjectResponse> get(Long userId, Long projectId) {
        try {
            ProjectResponse value = redisTemplate.opsForValue().get(buildKey(userId, projectId));
            return Optional.ofNullable(value);
        } catch (DataAccessException | SerializationException ex) {
            log.warn("Unable to read project {} from cache", projectId, ex);
            return Optional.empty();
        }
    }

    public void put(Long userId, ProjectResponse project) {
        try {
            redisTemplate.opsForValue().set(buildKey(userId, project.id()), project, TTL);
        } catch (DataAccessException | SerializationException ex) {
            log.warn("Unable to cache project {}", project.id(), ex);
        }
    }

    public void evict(Long userId, Long projectId) {
        try {
            redisTemplate.delete(buildKey(userId, projectId));
        } catch (DataAccessException ex) {
            log.warn("Unable to evict project {} from cache", projectId, ex);
        }
    }
}
