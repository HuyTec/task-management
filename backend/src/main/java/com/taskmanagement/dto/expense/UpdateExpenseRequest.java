package com.taskmanagement.dto.expense;

import java.time.LocalDate;

import com.taskmanagement.model.ExpenseCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateExpenseRequest(
    @NotBlank(message = "Description cannot be blank")
    String description,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    Double amount,

    @Positive(message = "Task ID must be positive")
    Long taskId,        

    ExpenseCategory category,   

    @NotNull(message = "Expense date is required")
    LocalDate expenseDate
) {

}
