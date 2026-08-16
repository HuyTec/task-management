package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.page.PageResponse;
import com.taskmanagement.dto.project.CreateProjectRequest;
import com.taskmanagement.dto.project.ProjectResponse;
import com.taskmanagement.dto.project.UpdateProjectRequest;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.ForbiddenException;
import com.taskmanagement.event.TaskCacheEvictEvent;
import com.taskmanagement.mapper.ProjectMapper;
import com.taskmanagement.model.Project;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.Task;
import com.taskmanagement.model.User;
import com.taskmanagement.repository.MemberRepository;
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
    @Mock private MemberRepository projectMemberRepository;
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

        verifyNoInteractions(projectRepository, projectMemberRepository, userRepository, securityUtils);
    }

    @Test
    void createProjectCreatesOwnerMembershipForCurrentUser() {
        Long userId = 7L;
        CreateProjectRequest request = new CreateProjectRequest(
                "Membership foundation",
                "Every project starts with an owner",
                LocalDate.of(2026, 8, 14),
                LocalDate.of(2026, 8, 31)
        );
        User owner = new User();
        owner.setId(userId);
        Project project = new Project();
        ProjectResponse mappedResponse = new ProjectResponse(
                42L,
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate()
        );

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(projectMapper.toProject(request)).thenReturn(project);
        when(projectMapper.toProjectResponse(project)).thenReturn(mappedResponse);

        Response<ProjectResponse> response = projectService.createProject(request);

        assertThat(response.data()).isEqualTo(new ProjectResponse(
                mappedResponse.id(), mappedResponse.name(), mappedResponse.description(),
                mappedResponse.startDate(), mappedResponse.endDate(), ProjectRole.OWNER
        ));
        assertThat(project.getUser()).isSameAs(owner);
        verify(projectRepository).save(project);
        verify(projectMemberRepository).save(argThat(membership ->
                membership.getProject() == project
                        && membership.getUser() == owner
                        && membership.getRole() == ProjectRole.OWNER
        ));
    }

    @Test
    void getMyProjectsReturnsProjectsFromCurrentUsersMemberships() {
        Long userId = 7L;
        Project sharedProject = new Project();
        sharedProject.setId(42L);
        User currentUserEntity = new User();
        currentUserEntity.setId(userId);
        com.taskmanagement.model.ProjectMember membership = membership(
                sharedProject, currentUserEntity, ProjectRole.MEMBER
        );
        ProjectResponse mappedResponse = new ProjectResponse(
                42L,
                "Shared project",
                "Joined as a member",
                LocalDate.of(2026, 8, 14),
                LocalDate.of(2026, 8, 31)
        );
        PageRequest pageable = PageRequest.of(0, 20);
        PageImpl<Project> projectPage = new PageImpl<>(List.of(sharedProject), pageable, 1);

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        when(projectRepository.findByUserId(userId, pageable)).thenReturn(projectPage);
        when(projectMemberRepository.findByProjectIdInAndUserId(List.of(42L), userId))
                .thenReturn(List.of(membership));
        when(projectMapper.toProjectResponse(sharedProject)).thenReturn(mappedResponse);

        Response<PageResponse<ProjectResponse>> response = projectService.getMyProjects(pageable);

        assertThat(response.data().content()).containsExactly(new ProjectResponse(
                mappedResponse.id(), mappedResponse.name(), mappedResponse.description(),
                mappedResponse.startDate(), mappedResponse.endDate(), ProjectRole.MEMBER
        ));
        assertThat(response.data().totalElements()).isEqualTo(1);
        verify(projectRepository).findByUserId(userId, pageable);
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
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(membership(new Project(), new User(), ProjectRole.MEMBER)));
        when(projectCacheService.get(userId, projectId)).thenReturn(Optional.of(cachedProject));

        Response<ProjectResponse> response = projectService.getProjectById(projectId);

        assertThat(response.data()).isEqualTo(new ProjectResponse(
                cachedProject.id(), cachedProject.name(), cachedProject.description(),
                cachedProject.startDate(), cachedProject.endDate(), ProjectRole.MEMBER
        ));
        verify(projectCacheService).get(userId, projectId);
        verify(projectMemberRepository).findByProjectIdAndUserId(projectId, userId);
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
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(membership(project, new User(), ProjectRole.OWNER)));

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
        User owner = new User();
        owner.setId(userId);
        com.taskmanagement.model.ProjectMember ownerMembership = membership(project, owner, ProjectRole.OWNER);
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(ownerMembership));
        when(projectMemberRepository.findByProjectId(projectId)).thenReturn(List.of(ownerMembership));
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

    @Test
    void regularMemberCannotUpdateProject() {
        Long userId = 7L;
        Long projectId = 42L;
        Project project = new Project();
        User member = new User();
        member.setId(userId);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(membership(project, member, ProjectRole.MEMBER)));

        assertThatThrownBy(() -> projectService.updateProject(
                projectId,
                new UpdateProjectRequest("Changed", null, null, null)
        )).isInstanceOf(ForbiddenException.class)
          .hasMessage("Project update requires OWNER or MANAGER role");

        verify(projectRepository, never()).save(project);
    }

    @Test
    void managerCannotDeleteProject() {
        Long userId = 7L;
        Long projectId = 42L;
        Project project = new Project();
        User manager = new User();
        manager.setId(userId);
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(membership(project, manager, ProjectRole.MANAGER)));

        assertThatThrownBy(() -> projectService.deleteProject(projectId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only the project OWNER can delete a project");

        verify(projectRepository, never()).delete(project);
        verifyNoInteractions(taskRepository);
    }

    private com.taskmanagement.model.ProjectMember membership(
            Project project,
            User user,
            ProjectRole role
    ) {
        com.taskmanagement.model.ProjectMember membership = new com.taskmanagement.model.ProjectMember();
        membership.setProject(project);
        membership.setUser(user);
        membership.setRole(role);
        return membership;
    }
}
