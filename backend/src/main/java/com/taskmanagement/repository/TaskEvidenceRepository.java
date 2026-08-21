package com.taskmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taskmanagement.model.TaskEvidence;
import com.taskmanagement.model.UploadStatus;

public interface TaskEvidenceRepository extends JpaRepository<TaskEvidence, Long> {
    List<TaskEvidence> findBySubmissionIdOrderByCreatedAtAsc(Long submissionId);
    Optional<TaskEvidence> findByIdAndSubmissionId(Long id, Long submissionId);
    long countBySubmissionId(Long submissionId);
    boolean existsBySubmissionIdAndUploadStatusNot(Long submissionId, UploadStatus uploadStatus);
}
