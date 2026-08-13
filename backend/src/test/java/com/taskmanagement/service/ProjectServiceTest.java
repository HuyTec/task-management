package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.project.CreateProjectRequest;
import com.taskmanagement.dto.project.ProjectResponse;
import com.taskmanagement.dto.project.UpdateProjectRequest;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.event.TaskCacheEvictEvent;
import com.taskmanagement.mapper.ProjectMapper;
import com.taskmanagement.model.Project;
import com.taskmanagement.model.Task;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.service.cache.ProjectCacheService;
import com.taskmanagement.service.project.ProjectService;
import com.taskmanagement.utils.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectCacheService projectCacheService;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private ProjectMapper projectMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CustomUserDetails currentUser;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProjectRejectsStartDateAfterEndDateBeforePersistence() {
        CreateProjectRequest request = new CreateProjectRequest(
                "Phase 02",
                null,
                LocalDate.of(2026, 8, 14),
                LocalDate.of(2026, 8, 13)
        );

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Start date must be on or before end date");

        verifyNoInteractions(projectRepository, userRepository, securityUtils);
    }

    @Test
    void getProjectByIdUsesCurrentUserInCacheKey() {
        Long userId = 7L;
        Long projectId = 42L;
        ProjectResponse cachedProject = new ProjectResponse(
                projectId,
                "Private project",
                null,
                LocalDate.of(2026, 8, 13),
                LocalDate.of(2026, 8, 20)
        );

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        when(projectCacheService.get(userId, projectId)).thenReturn(Optional.of(cachedProject));

        Response<ProjectResponse> response = projectService.getProjectById(projectId);

        assertThat(response.data()).isEqualTo(cachedProject);
        verify(projectCacheService).get(userId, projectId);
        verify(projectRepository, never()).findByIdAndUserId(projectId, userId);
    }

    @Test
    void updateProjectValidatesTheEffectiveDateRangeForPartialUpdates() {
        Long userId = 7L;
        Long projectId = 42L;
        Project project = new Project();
        project.setStartDate(LocalDate.of(2026, 8, 13));
        project.setEndDate(LocalDate.of(2026, 8, 20));
        UpdateProjectRequest request = new UpdateProjectRequest(
                null,
                null,
                null,
                LocalDate.of(2026, 8, 12)
        );

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.updateProject(projectId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Start date must be on or before end date");

        verify(projectRepository, never()).save(project);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void deleteProjectUnlinksTasksWithoutDeletingThem() {
        Long userId = 7L;
        Long projectId = 42L;
        Project project = new Project();
        Task firstTask = new Task();
        firstTask.setId(1L);
        firstTask.setProject(project);
        Task secondTask = new Task();
        secondTask.setId(2L);
        secondTask.setProject(project);

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        when(projectRepository.findByIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(project));
        when(taskRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(List.of(firstTask, secondTask));

        projectService.deleteProject(projectId);

        assertThat(firstTask.getProject()).isNull();
        assertThat(secondTask.getProject()).isNull();
        verify(taskRepository).saveAll(List.of(firstTask, secondTask));
        verify(projectRepository).delete(project);
        verify(taskRepository, never()).delete(firstTask);
        verify(taskRepository, never()).delete(secondTask);
        verify(eventPublisher).publishEvent(new TaskCacheEvictEvent(userId, 1L));
        verify(eventPublisher).publishEvent(new TaskCacheEvictEvent(userId, 2L));
    }
}
