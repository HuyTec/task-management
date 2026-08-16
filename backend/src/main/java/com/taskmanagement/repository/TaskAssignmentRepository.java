package com.taskmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taskmanagement.model.AssignmentStatus;
import com.taskmanagement.model.TaskAssignment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long>{

    boolean existsByTaskId(Long taskId);

    Optional<TaskAssignment> findByTaskIdAndStatus(Long taskId, AssignmentStatus status);

    boolean existsByTaskIdAndStatus(Long taskId, AssignmentStatus status);

    boolean existsByAssigneeIdAndStatus(Long assigneeId, AssignmentStatus status);

    Optional<TaskAssignment> findByTaskIdAndAssigneeIdAndStatus(Long taskId, Long assigneeId, AssignmentStatus status);

    List<TaskAssignment> findByTaskIdInAndStatus(Collection<Long> taskIds, AssignmentStatus status);
}
