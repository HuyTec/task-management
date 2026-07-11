package com.taskmanagement.dto.task;

import java.time.LocalDateTime;

import com.taskmanagement.model.TaskPriority;

public record CreateTaskRequest(
    String title,
    String description,
    TaskPriority priority,
    Long userId,
    LocalDateTime dueDate
) {
}
