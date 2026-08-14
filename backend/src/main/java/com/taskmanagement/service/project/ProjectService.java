package com.taskmanagement.service.project;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskmanagement.dto.Response;
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
import com.taskmanagement.repository.ProjectMemberRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.service.cache.ProjectCacheService;
import com.taskmanagement.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectCacheService projectCacheService;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ProjectMapper projectMapper;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    private Project ensureProjectAvailable(Long userId, Long projectId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found!"));
    }

    @Transactional(readOnly = true)
    public Response<ProjectResponse> getProjectById(Long id) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();

        Optional<ProjectResponse> cached = projectCacheService.get(currentUser.getId(), id);
        if (cached.isPresent()) {
            return Response.success(cached.get(), "Project data retrieved successfully!");
        }

        Project project = ensureProjectAvailable(currentUser.getId(), id);
        ProjectResponse response = projectMapper.toProjectResponse(project);
        projectCacheService.put(currentUser.getId(), response);

        return Response.success(response, "Project data retrieved successfully!");
    }

    @Transactional(readOnly = true)
    public Response<List<ProjectResponse>> getAllProjects() {
        if (!securityUtils.isAdmin(securityUtils.getAuthentication())) {
            throw new ForbiddenException("Only admin can view all projects");
        }

        List<ProjectResponse> responses = projectRepository.findAll().stream()
                .map(projectMapper::toProjectResponse)
                .toList();
        return Response.success(responses, "Project data retrieved successfully!");
    }

    @Transactional(readOnly = true)
    public Response<List<ProjectResponse>> getMyProjects() {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        List<ProjectResponse> responses = projectRepository.findByUserId(currentUser.getId()).stream()
                .map(projectMapper::toProjectResponse)
                .toList();
        return Response.success(responses, "Project data retrieved successfully!");
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
                projectMapper.toProjectResponse(project),
                "Project created successfully!"
        );
    }

    public Response<ProjectResponse> updateProject(Long id, UpdateProjectRequest request) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        Project project = ensureProjectAvailable(currentUser.getId(), id);

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
        eventPublisher.publishEvent(new ProjectCacheEvictEvent(currentUser.getId(), id));

        return Response.success(
                projectMapper.toProjectResponse(project),
                "Project updated successfully!"
        );
    }

    public Response<Void> deleteProject(Long id) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        Project project = ensureProjectAvailable(currentUser.getId(), id);

        List<Task> linkedTasks = taskRepository.findByProjectIdAndUserId(id, currentUser.getId());
        linkedTasks.forEach(task -> task.setProject(null));
        taskRepository.saveAll(linkedTasks);

        projectRepository.delete(project);
        linkedTasks.forEach(task -> eventPublisher.publishEvent(
                new TaskCacheEvictEvent(currentUser.getId(), task.getId())
        ));
        eventPublisher.publishEvent(new ProjectCacheEvictEvent(currentUser.getId(), id));

        return Response.success(null, "Project deleted successfully!");
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
