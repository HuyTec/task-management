package com.taskmanagement.dto.project;

import com.taskmanagement.model.ProjectRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddProjectMemberRequest(
        @NotBlank(message = "Username cannot be blank")
        @Size(max = 255, message = "Username must be at most 255 characters long")
        String username,

        @NotNull(message = "Project role is required")
        ProjectRole role
) {
}
