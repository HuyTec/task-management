package com.taskmanagement.dto.task;

import com.taskmanagement.model.TaskStatus;

public record TaskWorkflowResponse(
        Long taskId,
        TaskStatus status
) {
}
