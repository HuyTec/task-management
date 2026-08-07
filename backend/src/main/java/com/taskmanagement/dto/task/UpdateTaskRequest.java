package com.taskmanagement.dto.task;

import java.time.LocalDate;

import com.taskmanagement.model.TaskPriority;
import com.taskmanagement.model.TaskStatus;

public record UpdateTaskRequest(
    String title,
    String description,
    TaskPriority priority,
    TaskStatus status,
    LocalDate dueDate
) {

}
