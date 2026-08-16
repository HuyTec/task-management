package com.taskmanagement.dto.task;

import java.time.LocalDateTime;

import com.taskmanagement.model.AssignmentStatus;
import com.taskmanagement.model.AssignmentType;

public record TaskAssignmentResponse(
        Long id,
        Long taskId,
        String assigneeUsername,
        String assigneeDisplayName,
        String assignedByUsername,
        AssignmentType type,
        AssignmentStatus status,
        LocalDateTime assignedAt,
        LocalDateTime endedAt
) {
}
