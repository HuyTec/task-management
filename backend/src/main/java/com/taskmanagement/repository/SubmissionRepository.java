package com.taskmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import com.taskmanagement.model.Submission;
import com.taskmanagement.model.SubmissionStatus;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Submission s WHERE s.id = :submissionId")
    Optional<Submission> findByIdForUpdate(@Param("submissionId") Long submissionId);

    List<Submission> findByTaskIdOrderBySequenceNumberDesc(Long taskId);

    Optional<Submission> findFirstByTaskIdAndStatusOrderBySequenceNumberDesc(
            Long taskId,
            SubmissionStatus status
    );

    boolean existsByTaskIdAndStatus(Long taskId, SubmissionStatus status);

    @Query("SELECT COALESCE(MAX(s.sequenceNumber), 0) FROM Submission s WHERE s.task.id = :taskId")
    int findMaxSequenceNumber(@Param("taskId") Long taskId);
}
