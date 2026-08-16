package com.taskmanagement.dto.task;

import java.time.LocalDateTime;

import com.taskmanagement.model.ReviewDecision;
import com.taskmanagement.model.TaskStatus;

public record TaskReviewResponse(
        Long id,
        Long taskId,
        ReviewDecision decision,
        String message,
        String reviewerUsername,
        TaskStatus taskStatus,
        LocalDateTime createdAt
) {
}
