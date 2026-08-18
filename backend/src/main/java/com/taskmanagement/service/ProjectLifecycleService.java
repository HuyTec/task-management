package com.taskmanagement.service;

import static com.taskmanagement.model.ProjectStatus.ACTIVE;
import static com.taskmanagement.model.ProjectStatus.ARCHIVED;
import static com.taskmanagement.model.ProjectStatus.COMPLETED;
import static com.taskmanagement.model.ProjectStatus.ON_HOLD;
import static com.taskmanagement.model.ProjectStatus.PLANNING;

import java.util.Map;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskmanagement.dto.Response;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.ForbiddenException;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.model.Project;
import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.ProjectStatus;
import com.taskmanagement.model.Task;
import com.taskmanagement.model.TaskStatus;
import com.taskmanagement.repository.MemberRepository;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectLifecycleService {

    private static final Map<ProjectStatus, Set<ProjectStatus>> ALLOWED_TRANSITIONS = Map.of(
            PLANNING, Set.of(ACTIVE),
            ACTIVE, Set.of(ON_HOLD, COMPLETED),
            ON_HOLD, Set.of(ACTIVE),
            COMPLETED, Set.of(ARCHIVED),
            ARCHIVED, Set.of()
    );

    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final TaskRepository taskRepository;
    private final SecurityUtils securityUtils;

    public Response<ProjectStatus> activate(Long projectId) {
        return transition(
                projectId, PLANNING, ACTIVE, false, "activate", "Project activated successfully!"
        );
    }

    public Response<ProjectStatus> hold(Long projectId) {
        return transition(
                projectId, ACTIVE, ON_HOLD, false, "hold", "Project put on hold successfully!"
        );
    }

    public Response<ProjectStatus> resume(Long projectId) {
        return transition(
                projectId, ON_HOLD, ACTIVE, false, "resume", "Project resumed successfully!"
        );
    }

    public Response<ProjectStatus> complete(Long projectId) {
        return transition(
                projectId, ACTIVE, COMPLETED, false, "complete", "Project completed successfully!"
        );
    }

    public Response<ProjectStatus> archive(Long projectId) {
        return transition(
                projectId, COMPLETED, ARCHIVED, true, "archive", "Project archived successfully!"
        );
    }

    private Response<ProjectStatus> transition(
            Long projectId,
            ProjectStatus requiredCurrentStatus,
            ProjectStatus targetStatus,
            boolean ownerOnly,
            String command,
            String successMessage
    ) {
        ProjectMember actor = requireAuthorizedMember(projectId, ownerOnly);
        Project project = actor.getProject();
        ProjectStatus currentStatus = project.getStatus();

        if (currentStatus != requiredCurrentStatus
                || !ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus)) {
            throw new BadRequestException(
                    "Project command " + command + " requires status "
                            + requiredCurrentStatus + " but was " + currentStatus
            );
        }

        if (targetStatus == COMPLETED) {
            requireAllTasksDone(projectId);
        }

        project.setStatus(targetStatus);
        projectRepository.save(project);
        return Response.success(targetStatus, successMessage);
    }

    private ProjectMember requireAuthorizedMember(Long projectId, boolean ownerOnly) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        ProjectMember membership = memberRepository
                .findByProjectIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found!"));

        if (ownerOnly && membership.getRole() != ProjectRole.OWNER) {
            throw new ForbiddenException("Only the project OWNER can archive a project");
        }
        if (!ownerOnly
                && membership.getRole() != ProjectRole.OWNER
                && membership.getRole() != ProjectRole.MANAGER) {
            throw new ForbiddenException(
                    "Project lifecycle transition requires OWNER or MANAGER role"
            );
        }
        return membership;
    }

    private void requireAllTasksDone(Long projectId) {
        long taskCount = taskRepository.count(tasksInProject(projectId));
        if (taskCount == 0) {
            throw new BadRequestException("Project requires at least one task before completion");
        }

        long incompleteTaskCount = taskRepository.count(
                tasksInProjectWithStatusOtherThan(projectId, TaskStatus.DONE)
        );
        if (incompleteTaskCount > 0) {
            throw new BadRequestException("All project tasks must be DONE before completion");
        }
    }

    private Specification<Task> tasksInProject(Long projectId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("project").get("id"),
                projectId
        );
    }

    private Specification<Task> tasksInProjectWithStatusOtherThan(
            Long projectId,
            TaskStatus excludedStatus
    ) {
        return tasksInProject(projectId).and((root, query, criteriaBuilder) ->
                criteriaBuilder.notEqual(root.get("status"), excludedStatus)
        );
    }
}
