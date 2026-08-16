package com.taskmanagement.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignTaskRequest(
        @NotBlank(message = "Assignee username cannot be blank")
        @Size(max = 255, message = "Assignee username must be at most 255 characters")
        String username
) {
}
