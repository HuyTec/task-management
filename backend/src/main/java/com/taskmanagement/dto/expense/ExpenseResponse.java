package com.taskmanagement.dto.expense;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseResponse(
    Long id,
    String description,
    Double amount,
    Long userId,
    Long taskId,
    Long categoryId,
    LocalDateTime createdAt,
    LocalDate expenseDate
) {
}