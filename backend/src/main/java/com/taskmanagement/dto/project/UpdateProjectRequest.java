package com.taskmanagement.dto.project;
import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 100, message = "Project name must be less than 100 characters")
        String name,

        @Size(max = 255, message = "Description must be less than 255 characters")
        String description,

        LocalDate startDate,
        LocalDate endDate

) {

}