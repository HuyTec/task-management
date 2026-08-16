package com.taskmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taskmanagement.model.TaskAcceptanceCriterion;

public interface TaskAcceptanceCriterionRepository
        extends JpaRepository<TaskAcceptanceCriterion, Long> {

    boolean existsByTaskId(Long taskId);

    Optional<TaskAcceptanceCriterion> findByIdAndTaskId(Long id, Long taskId);

    List<TaskAcceptanceCriterion> findByTaskIdOrderByPositionAsc(Long taskId);

    boolean existsByTaskIdAndSatisfiedFalse(Long taskId);
}
