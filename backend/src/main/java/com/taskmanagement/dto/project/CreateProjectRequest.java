package com.taskmanagement.dto.project;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "Project name cannot be blank")
        @Size(max = 100, message = "Project name must be less than 100 characters")
        String name,

        @Size(max = 255, message = "Description must be less than 255 characters")
        String description,

        @NotNull(message = "Start date cannot be null")
        LocalDate startDate,

        @NotNull(message = "End date cannot be null")
        LocalDate endDate

        
) {}
