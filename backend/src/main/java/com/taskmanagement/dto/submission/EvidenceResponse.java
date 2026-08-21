package com.taskmanagement.dto.submission;

import java.time.LocalDateTime;

import com.taskmanagement.model.EvidenceProvider;
import com.taskmanagement.model.EvidenceType;
import com.taskmanagement.model.UploadStatus;

public record EvidenceResponse(
        Long id,
        EvidenceType evidenceType,
        EvidenceProvider provider,
        String displayName,
        String url,
        String contentType,
        Long fileSize,
        UploadStatus uploadStatus,
        LocalDateTime createdAt
) {
}
