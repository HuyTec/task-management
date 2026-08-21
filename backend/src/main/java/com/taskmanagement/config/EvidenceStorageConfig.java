package com.taskmanagement.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.taskmanagement.exception.StorageUnavailableException;
import com.taskmanagement.service.submission.EvidenceStorage;

@Configuration
public class EvidenceStorageConfig {
    @Bean
    @ConditionalOnMissingBean(EvidenceStorage.class)
    EvidenceStorage unavailableEvidenceStorage() {
        return new EvidenceStorage() {
            @Override
            public PresignedUpload createUpload(String storageKey, String contentType, long fileSize) {
                throw new StorageUnavailableException("Evidence storage provider is not configured");
            }

            @Override
            public StoredObject verifyUpload(String storageKey) {
                throw new StorageUnavailableException("Evidence storage provider is not configured");
            }
        };
    }
}
