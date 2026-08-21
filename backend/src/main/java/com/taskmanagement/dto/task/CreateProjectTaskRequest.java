package com.taskmanagement.dto.task;

import java.time.LocalDate;
import java.util.List;

import com.taskmanagement.model.TaskPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProjectTaskRequest(
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 160, message = "Title must be at most 160 characters")
        String title,
        @NotBlank(message = "Description cannot be blank")
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,
        @NotNull(message = "Priority is required")
        TaskPriority priority,
        @NotNull(message = "Due date is required")
        LocalDate dueDate,
        @NotEmpty(message = "At least one acceptance criterion is required")
        @Size(max = 20, message = "A project task can have at most 20 acceptance criteria")
        List<@NotBlank(message = "Criterion content cannot be blank")
             @Size(max = 1000, message = "Criterion content must be at most 1000 characters") String> criteria,
        @Size(max = 255, message = "Assignee username must be at most 255 characters")
        String assigneeUsername
) {
}
