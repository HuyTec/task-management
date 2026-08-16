package com.taskmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.taskmanagement.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("""
        SELECT DISTINCT p
        FROM Project p
        JOIN ProjectMember pm ON pm.project.id = p.id
        WHERE pm.user.id = :userId
        ORDER BY p.createdAt DESC
        """)
    Page<Project> findByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
        SELECT p
        FROM Project p
        JOIN ProjectMember pm ON pm.project.id = p.id
        WHERE p.id = :projectId
        AND pm.user.id = :userId
    """)
    Optional<Project> findAccessibleProject(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId
    );

}
