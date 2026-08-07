package com.taskmanagement.dto.task;

import java.time.LocalDate;

import com.taskmanagement.model.TaskPriority;
import com.taskmanagement.model.TaskStatus;

public record TaskResponse(
    Long id,
    String title,
    TaskStatus status,
    TaskPriority priority,
    LocalDate dueDate,
    Double total
) {

}
