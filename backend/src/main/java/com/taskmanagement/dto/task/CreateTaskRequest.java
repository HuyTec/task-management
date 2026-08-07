package com.taskmanagement.dto.task;

import java.time.LocalDate;

import com.taskmanagement.model.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTaskRequest(
    @NotBlank(message = "Title cannot be blank")
    String title,
    String description,
    @NotNull(message = "Priority is required")
    TaskPriority priority,
    LocalDate dueDate
) {
}
