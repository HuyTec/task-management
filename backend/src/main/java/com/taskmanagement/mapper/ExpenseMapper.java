package com.taskmanagement.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.taskmanagement.dto.expense.ExpenseResponse;
import com.taskmanagement.model.Expense;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {
    
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "categoryId", source = "expenseCategory.id")
    public ExpenseResponse toExpenseResponse(Expense expense);

}
