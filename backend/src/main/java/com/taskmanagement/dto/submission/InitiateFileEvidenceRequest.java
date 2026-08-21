package com.taskmanagement.dto.submission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record InitiateFileEvidenceRequest(
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank @Size(max = 255) String contentType,
        @NotNull @Positive Long fileSize
) {
}
