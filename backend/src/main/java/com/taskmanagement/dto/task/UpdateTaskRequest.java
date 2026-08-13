package com.taskmanagement.dto.task;

import java.time.LocalDate;

import com.taskmanagement.model.TaskPriority;
import com.taskmanagement.model.TaskStatus;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateTaskRequest(
    String title,
    String description,
    TaskPriority priority,
    TaskStatus status,
    LocalDate dueDate,
    @PositiveOrZero(message = "Project id must be zero or positive") Long projectId
) {

}
