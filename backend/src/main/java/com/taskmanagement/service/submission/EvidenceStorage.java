package com.taskmanagement.service.submission;

import java.util.Map;

public interface EvidenceStorage {
    PresignedUpload createUpload(String storageKey, String contentType, long fileSize);
    StoredObject verifyUpload(String storageKey);

    record PresignedUpload(String uploadUrl, Map<String, String> requiredHeaders) {}
    record StoredObject(long fileSize, String contentType) {}
}
