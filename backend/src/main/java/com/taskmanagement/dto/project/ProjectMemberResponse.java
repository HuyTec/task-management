package com.taskmanagement.dto.project;

import java.time.LocalDateTime;

import com.taskmanagement.model.ProjectRole;

public record ProjectMemberResponse(
        String username,
        String displayName,
        String email,
        ProjectRole role,
        LocalDateTime joinedAt
) {
}
