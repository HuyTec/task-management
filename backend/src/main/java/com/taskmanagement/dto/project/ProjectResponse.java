package com.taskmanagement.dto.project;

import java.time.LocalDate;

import com.taskmanagement.model.ProjectRole;

public record ProjectResponse(
    Long id,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    ProjectRole currentUserRole
) {
    public ProjectResponse(
            Long id,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this(id, name, description, startDate, endDate, null);
    }

}
