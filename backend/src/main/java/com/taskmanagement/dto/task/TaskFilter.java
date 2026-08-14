package com.taskmanagement.dto.task;
import com.taskmanagement.model.TaskPriority;
import com.taskmanagement.model.TaskStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskFilter(
    @Size(max = 100, message = "Search term must not exceed 100 characters")
    String search,

    TaskStatus status,

    TaskPriority priority,

    @Positive(message = "Project ID must be a positive number")
    Long projectId,

    LocalDate dueFrom,

    LocalDate dueTo
) {}
