package com.taskmanagement.service.project;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.page.PageResponse;
import com.taskmanagement.dto.project.CreateProjectRequest;
import com.taskmanagement.dto.project.ProjectResponse;
import com.taskmanagement.dto.project.UpdateProjectRequest;
import com.taskmanagement.event.ProjectCacheEvictEvent;
import com.taskmanagement.event.TaskCacheEvictEvent;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.ForbiddenException;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.mapper.ProjectMapper;
import com.taskmanagement.model.Project;
import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.Task;
import com.taskmanagement.model.User;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.MemberRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.service.cache.ProjectCacheService;
import com.taskmanagement.utils.SecurityUtils;
import com.taskmanagement.utils.PageableValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectService {
    private static final Set<String> ALLOWED_SORTS = Set.of(
            "name", "startDate", "endDate", "createdAt", "updatedAt"
    );

    private final ProjectCacheService projectCacheService;
    private final ProjectRepository projectRepository;
    private final MemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ProjectMapper projectMapper;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    private ProjectMember requireMembership(Long userId, Long projectId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found!"));
    }

    private ProjectResponse withRole(ProjectResponse project, ProjectRole role) {
        return new ProjectResponse(
                project.id(), project.name(), project.description(),
                project.startDate(), project.endDate(), role
        );
    }

    @Transactional(readOnly = true)
    public Response<ProjectResponse> getProjectById(Long id) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        ProjectMember membership = requireMembership(currentUser.getId(), id);

        Optional<ProjectResponse> cached = projectCacheService.get(currentUser.getId(), id);
        if (cached.isPresent()) {
            return Response.success(
                    withRole(cached.get(), membership.getRole()),
                    "Project data retrieved successfully!"
            );
        }

        ProjectResponse response = withRole(
                projectMapper.toProjectResponse(membership.getProject()),
                membership.getRole()
        );
        projectCacheService.put(currentUser.getId(), response);

        return Response.success(response, "Project data retrieved successfully!");
    }

    @Transactional(readOnly = true)
    public Response<PageResponse<ProjectResponse>> getAllProjects(Pageable pageable) {
        if (!securityUtils.isAdmin(securityUtils.getAuthentication())) {
            throw new ForbiddenException("Only admin can view all projects");
        }
        PageableValidator.requireAllowedSorts(pageable, ALLOWED_SORTS);

        Page<Project> projects = projectRepository.findAll(pageable);
        List<ProjectResponse> responses = projects.getContent().stream()
                .map(projectMapper::toProjectResponse)
                .toList();
        return Response.success(PageResponse.from(projects, responses), "Project data retrieved successfully!");
    }

    @Transactional(readOnly = true)
    public Response<PageResponse<ProjectResponse>> getMyProjects(Pageable pageable) {
        PageableValidator.requireAllowedSorts(pageable, ALLOWED_SORTS);
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        Page<Project> projects = projectRepository.findByUserId(currentUser.getId(), pageable);
        List<Long> projectIds = projects.getContent().stream().map(Project::getId).toList();
        Map<Long, ProjectMember> memberships = projectIds.isEmpty()
                ? Map.of()
                : projectMemberRepository.findByProjectIdInAndUserId(projectIds, currentUser.getId()).stream()
                        .collect(Collectors.toMap(member -> member.getProject().getId(), Function.identity()));
        List<ProjectResponse> responses = projects.getContent().stream()
                .map(project -> withRole(
                        projectMapper.toProjectResponse(project),
                        memberships.get(project.getId()).getRole()
                ))
                .toList();
        return Response.success(PageResponse.from(projects, responses), "Project data retrieved successfully!");
    }

    public Response<ProjectResponse> createProject(CreateProjectRequest request) {
        validateDateRange(request.startDate(), request.endDate());

        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        Project project = projectMapper.toProject(request);
        project.setUser(user);
        projectRepository.save(project);

        ProjectMember ownerMembership = new ProjectMember();
        ownerMembership.setProject(project);
        ownerMembership.setUser(user);
        ownerMembership.setRole(ProjectRole.OWNER);
        projectMemberRepository.save(ownerMembership);

        return Response.success(
                withRole(projectMapper.toProjectResponse(project), ProjectRole.OWNER),
                "Project created successfully!"
        );
    }

    public Response<ProjectResponse> updateProject(Long id, UpdateProjectRequest request) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        ProjectMember membership = requireMembership(currentUser.getId(), id);
        if (membership.getRole() != ProjectRole.OWNER && membership.getRole() != ProjectRole.MANAGER) {
            log.warn("Denied project update: actorUserId={}, projectId={}, actorRole={}",
                    currentUser.getId(), id, membership.getRole());
            throw new ForbiddenException("Project update requires OWNER or MANAGER role");
        }
        Project project = membership.getProject();

        LocalDate nextStartDate = request.startDate() != null
                ? request.startDate()
                : project.getStartDate();
        LocalDate nextEndDate = request.endDate() != null
                ? request.endDate()
                : project.getEndDate();
        validateDateRange(nextStartDate, nextEndDate);

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BadRequestException("Project name cannot be blank");
            }
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        project.setStartDate(nextStartDate);
        project.setEndDate(nextEndDate);

        projectRepository.save(project);
        evictProjectForMembers(id);

        return Response.success(
                withRole(projectMapper.toProjectResponse(project), membership.getRole()),
                "Project updated successfully!"
        );
    }

    public Response<Void> deleteProject(Long id) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        ProjectMember membership = requireMembership(currentUser.getId(), id);
        if (membership.getRole() != ProjectRole.OWNER) {
            log.warn("Denied project deletion: actorUserId={}, projectId={}, actorRole={}",
                    currentUser.getId(), id, membership.getRole());
            throw new ForbiddenException("Only the project OWNER can delete a project");
        }
        Project project = membership.getProject();
        List<Long> memberUserIds = projectMemberRepository.findByProjectId(id).stream()
                .map(member -> member.getUser().getId())
                .toList();

        List<Task> linkedTasks = taskRepository.findByProjectIdAndUserId(id, currentUser.getId());
        linkedTasks.forEach(task -> task.setProject(null));
        taskRepository.saveAll(linkedTasks);

        projectRepository.delete(project);
        linkedTasks.forEach(task -> eventPublisher.publishEvent(
                new TaskCacheEvictEvent(currentUser.getId(), task.getId())
        ));
        memberUserIds.forEach(userId -> eventPublisher.publishEvent(new ProjectCacheEvictEvent(userId, id)));

        return Response.success(null, "Project deleted successfully!");
    }

    private void evictProjectForMembers(Long projectId) {
        projectMemberRepository.findByProjectId(projectId).forEach(member ->
                eventPublisher.publishEvent(new ProjectCacheEvictEvent(member.getUser().getId(), projectId))
        );
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be on or before end date");
        }
    }
}
