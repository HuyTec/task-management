package com.taskmanagement.dto.task;

import com.taskmanagement.dto.expense.ExpenseResponse;
import com.taskmanagement.model.TaskPriority;
import com.taskmanagement.model.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;

public record TaskDetailResponse(
    Long id,
    String title,
    String description,
    TaskStatus status,
    TaskPriority priority,
    Long userId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime dueDate,
    List<ExpenseResponse> expenses,
    Double total
) {
}
