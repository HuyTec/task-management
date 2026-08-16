package com.taskmanagement.dto.project;

import com.taskmanagement.model.ProjectRole;

import jakarta.validation.constraints.NotNull;

public record UpdateProjectMemberRoleRequest(
        @NotNull(message = "Project role is required")
        ProjectRole role
) {
}
