package com.taskmanagement.dto.expense;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.taskmanagement.model.ExpenseCategory;

public record ExpenseResponse(
    Long id,
    String description,
    Double amount,
    Long userId,
    Long taskId,
    ExpenseCategory category,
    LocalDateTime createdAt,
    LocalDate expenseDate
) {
}