package com.taskmanagement.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.taskmanagement.dto.expense.CreateExpenseRequest;
import com.taskmanagement.dto.expense.ExpenseResponse;
import com.taskmanagement.model.Expense;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {
    
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "taskId", source = "task.id")
    public ExpenseResponse toExpenseResponse(Expense expense);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "task", ignore = true)
    public Expense toExpense(CreateExpenseRequest request);
}
