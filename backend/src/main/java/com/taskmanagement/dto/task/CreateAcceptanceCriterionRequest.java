package com.taskmanagement.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateAcceptanceCriterionRequest(
        @NotBlank(message = "Criterion content cannot be blank")
        @Size(max = 1000, message = "Criterion content must be at most 1000 characters")
        String content,

        @NotNull(message = "Criterion position is required")
        @PositiveOrZero(message = "Criterion position must be zero or positive")
        Integer position
) {
}
