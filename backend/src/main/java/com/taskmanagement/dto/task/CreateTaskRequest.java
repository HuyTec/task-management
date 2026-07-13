package com.taskmanagement.dto.task;

import java.time.LocalDate;

import com.taskmanagement.model.TaskPriority;

public record CreateTaskRequest(
    String title,
    String description,
    TaskPriority priority,
    LocalDate dueDate
) {
}
