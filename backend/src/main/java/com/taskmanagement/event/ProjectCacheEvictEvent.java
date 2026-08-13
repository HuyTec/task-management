package com.taskmanagement.event;

public record ProjectCacheEvictEvent(Long userId, Long projectId) {
}
