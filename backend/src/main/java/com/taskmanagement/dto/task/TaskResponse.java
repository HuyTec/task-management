package com.taskmanagement.dto.task;

import java.time.LocalDate;

import com.taskmanagement.model.TaskPriority;
import com.taskmanagement.model.TaskStatus;
import com.taskmanagement.model.AssignmentType;
import com.taskmanagement.model.ProjectRole;

public record TaskResponse(
    Long id,
    String title,
    TaskStatus status,
    TaskPriority priority,
    LocalDate dueDate,
    Long projectId,
    Double total,
    String assigneeUsername,
    String assigneeDisplayName,
    AssignmentType assignmentType,
    ProjectRole currentUserRole
) {

    public TaskResponse(
            Long id,
            String title,
            TaskStatus status,
            TaskPriority priority,
            LocalDate dueDate,
            Long projectId,
            Double total
    ) {
        this(id, title, status, priority, dueDate, projectId, total, null, null, null, null);
    }

}
