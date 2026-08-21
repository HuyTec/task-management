package com.taskmanagement.dto.submission;

import java.util.Map;

public record FileUploadInitiatedResponse(
        EvidenceResponse evidence,
        String uploadUrl,
        Map<String, String> requiredHeaders
) {
}
