package com.taskmanagement.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestChangesRequest(
        @NotBlank(message = "Change request message cannot be blank")
        @Size(max = 5000, message = "Change request message must be at most 5000 characters")
        String message
) {
}
