package com.taskmanagement.dto.task;

public record AcceptanceCriterionResponse(
        Long id,
        String content,
        boolean satisfied,
        Integer position
) {
}
