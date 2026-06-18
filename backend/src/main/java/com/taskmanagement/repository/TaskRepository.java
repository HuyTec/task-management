package com.taskmanagement.repository;
import com.taskmanagement.model.Task;
import com.taskmanagement.model.TaskStatus;
import com.taskmanagement.model.TaskPriority;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserId(Long userId);

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId" 
    + " AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')))"
    + " AND (:status IS NULL OR t.status = :status)"
    + " AND (:priority IS NULL OR t.priority = :priority)" 
    + " AND (:dueDate IS NULL OR t.dueDate = :dueDate)")
    List<Task> findByFilter(@Param("userId") Long userId, 
                            @Param("status") TaskStatus status, 
                            @Param("priority") TaskPriority priority,
                            @Param("dueDate") LocalDate dueDate,
                            @Param("keyword") String keyword);

    List<Task> findByUserIdAndDueDateBefore(Long userId, LocalDate dueDate);
    List<Task> findByUserIdAndDueDateAfter(Long userId, LocalDate dueDate);
}
