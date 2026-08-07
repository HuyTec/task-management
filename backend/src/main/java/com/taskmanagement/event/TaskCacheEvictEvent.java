package com.taskmanagement.event;

public record TaskCacheEvictEvent(Long userId, Long taskId) {

}
