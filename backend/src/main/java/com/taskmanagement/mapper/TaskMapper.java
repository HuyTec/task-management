package com.taskmanagement.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.taskmanagement.dto.expense.ExpenseResponse;
import com.taskmanagement.dto.task.CreateTaskRequest;
import com.taskmanagement.dto.task.TaskDetailResponse;
import com.taskmanagement.dto.task.TaskResponse;
import com.taskmanagement.model.Task;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    @Mapping(target = "total", source = "total")
    public TaskResponse toTaskResponse(Task task, Double total);
    

    @Mapping(target = "userId", source = "task.user.id")
    @Mapping(target = "expenses", source = "expenses")
    @Mapping(target = "total", source = "total")
    public TaskDetailResponse toTaskDetailResponse(Task task, List<ExpenseResponse> expenses, Double total);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    public Task toTask(CreateTaskRequest request);
}

