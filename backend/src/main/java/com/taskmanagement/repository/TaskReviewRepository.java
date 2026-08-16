package com.taskmanagement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taskmanagement.model.TaskReview;

public interface TaskReviewRepository extends JpaRepository<TaskReview, Long> {
    boolean existsByTaskId(Long taskId);

    List<TaskReview> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}
