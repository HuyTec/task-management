package com.taskmanagement.dto.expense;

import java.time.LocalDate;

import com.taskmanagement.model.ExpenseCategory;
import jakarta.validation.constraints.Positive;

public record UpdateExpenseRequest(
    String description,
    @Positive(message = "Amount must be positive")
    Double amount,
    @Positive(message = "Task ID must be positive")
    Long taskId,
    ExpenseCategory category,
    LocalDate expenseDate
) {

}
