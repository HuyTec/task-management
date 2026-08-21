package com.taskmanagement.dto.submission;

import java.time.LocalDateTime;
import java.util.List;

import com.taskmanagement.model.SubmissionStatus;

public record SubmissionResponse(
        Long id,
        Long taskId,
        int sequenceNumber,
        SubmissionStatus status,
        String assigneeUsername,
        LocalDateTime createdAt,
        LocalDateTime submittedAt,
        List<EvidenceResponse> evidences
) {
}
