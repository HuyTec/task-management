package com.taskmanagement.dto.submission;

import com.taskmanagement.model.EvidenceProvider;
import com.taskmanagement.model.EvidenceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLinkEvidenceRequest(
        @NotNull EvidenceType evidenceType,
        @NotNull EvidenceProvider provider,
        @NotBlank @Size(max = 255) String displayName,
        @NotBlank @Size(max = 2048) String url
) {
}
