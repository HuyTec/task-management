package com.taskmanagement.dto.project;
import java.time.LocalDate;
public record ProjectResponse(
    Long id,
    String name,
    String description,
    LocalDate startDate,
    LocalDate endDate
) {

}
