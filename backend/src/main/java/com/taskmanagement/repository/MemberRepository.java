package com.taskmanagement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;

public interface MemberRepository extends JpaRepository<ProjectMember, Long> {
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    Optional<ProjectMember> findByProjectIdAndUserUsername(Long projectId, String username);

    List<ProjectMember> findByProjectId(Long projectId);
    Page<ProjectMember> findByProjectId(Long projectId, Pageable pageable);

    List<ProjectMember> findByProjectIdInAndUserId(List<Long> projectIds, Long userId);

    @Query("""
            SELECT membership
            FROM ProjectMember membership
            JOIN membership.user memberUser
            WHERE membership.project.id = :projectId
              AND (
                    LOWER(memberUser.username) LIKE CONCAT('%', :search, '%')
                    OR LOWER(memberUser.displayName) LIKE CONCAT('%', :search, '%')
                    OR LOWER(memberUser.email) LIKE CONCAT('%', :search, '%')
              )
            """)
    Page<ProjectMember> findByProjectIdAndSearch(
            @Param("projectId") Long projectId,
            @Param("search") String search,
            Pageable pageable
    );

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    boolean existsByProjectIdAndRole(Long projectId, ProjectRole role);
}
