package com.taskmanagement.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.taskmanagement.model.Project;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserId(Long userId);
    Optional<Project> findByIdAndUserId(Long id, Long userId);

}
