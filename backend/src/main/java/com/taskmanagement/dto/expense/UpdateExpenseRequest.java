package com.taskmanagement.dto.expense;

import java.time.LocalDate;

import com.taskmanagement.model.ExpenseCategory;

public record UpdateExpenseRequest(
    String description,
    Double amount,
    Long taskId,
    ExpenseCategory category,
    LocalDate expenseDate
) {

}
