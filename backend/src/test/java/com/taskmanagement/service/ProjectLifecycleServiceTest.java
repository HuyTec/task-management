package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import com.taskmanagement.dto.Response;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.ForbiddenException;
import com.taskmanagement.model.Project;
import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.ProjectStatus;
import com.taskmanagement.model.Task;
import com.taskmanagement.repository.MemberRepository;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.utils.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class ProjectLifecycleServiceTest {

    private static final Long PROJECT_ID = 42L;
    private static final Long USER_ID = 7L;

    @Mock private ProjectRepository projectRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private CustomUserDetails currentUser;

    @InjectMocks
    private ProjectLifecycleService lifecycleService;

    @ParameterizedTest
    @MethodSource("validTransitions")
    void acceptsEveryValidStateTransition(
            ProjectStatus currentStatus,
            ProjectStatus targetStatus,
            ProjectRole actorRole
    ) {
        Project project = authorizedProject(currentStatus, actorRole);
        if (targetStatus == ProjectStatus.COMPLETED) {
            when(taskRepository.count(anyTaskSpecification())).thenReturn(1L, 0L);
        }

        Response<ProjectStatus> response = execute(currentStatus, targetStatus);

        assertThat(response.data()).isEqualTo(targetStatus);
        assertThat(project.getStatus()).isEqualTo(targetStatus);
        verify(projectRepository).save(project);
    }

    @ParameterizedTest
    @MethodSource("invalidTransitions")
    void rejectsEveryInvalidStateTransition(
            ProjectStatus currentStatus,
            ProjectStatus targetStatus
    ) {
        Project project = authorizedProject(currentStatus, ProjectRole.OWNER);

        assertThatThrownBy(() -> execute(currentStatus, targetStatus))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("but was " + currentStatus);

        assertThat(project.getStatus()).isEqualTo(currentStatus);
        verify(projectRepository, never()).save(project);
        verifyNoInteractions(taskRepository);
    }

    @Test
    void completeRejectsProjectWithoutTasks() {
        Project project = authorizedProject(ProjectStatus.ACTIVE, ProjectRole.MANAGER);
        when(taskRepository.count(anyTaskSpecification())).thenReturn(0L);

        assertThatThrownBy(() -> lifecycleService.complete(PROJECT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Project requires at least one task before completion");

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        verify(projectRepository, never()).save(project);
    }

    @Test
    void completeRejectsProjectWithAnyTaskNotDone() {
        Project project = authorizedProject(ProjectStatus.ACTIVE, ProjectRole.OWNER);
        when(taskRepository.count(anyTaskSpecification())).thenReturn(3L, 1L);

        assertThatThrownBy(() -> lifecycleService.complete(PROJECT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("All project tasks must be DONE before completion");

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        verify(projectRepository, never()).save(project);
    }

    @Test
    void memberCannotPerformLifecycleTransition() {
        Project project = authorizedProject(ProjectStatus.PLANNING, ProjectRole.MEMBER);

        assertThatThrownBy(() -> lifecycleService.activate(PROJECT_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Project lifecycle transition requires OWNER or MANAGER role");

        verify(projectRepository, never()).save(project);
    }

    @Test
    void managerCannotArchiveProject() {
        Project project = authorizedProject(ProjectStatus.COMPLETED, ProjectRole.MANAGER);

        assertThatThrownBy(() -> lifecycleService.archive(PROJECT_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only the project OWNER can archive a project");

        verify(projectRepository, never()).save(project);
    }

    @Test
    void activateCannotBeUsedAsResumeCommand() {
        Project project = authorizedProject(ProjectStatus.ON_HOLD, ProjectRole.OWNER);

        assertThatThrownBy(() -> lifecycleService.activate(PROJECT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Project command activate requires status PLANNING but was ON_HOLD");

        verify(projectRepository, never()).save(project);
    }

    @Test
    void resumeCannotBeUsedAsActivateCommand() {
        Project project = authorizedProject(ProjectStatus.PLANNING, ProjectRole.MANAGER);

        assertThatThrownBy(() -> lifecycleService.resume(PROJECT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Project command resume requires status ON_HOLD but was PLANNING");

        verify(projectRepository, never()).save(project);
    }

    private Project authorizedProject(ProjectStatus status, ProjectRole role) {
        Project project = new Project();
        project.setId(PROJECT_ID);
        project.setStatus(status);
        ProjectMember membership = new ProjectMember();
        membership.setProject(project);
        membership.setRole(role);

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(USER_ID);
        when(memberRepository.findByProjectIdAndUserId(PROJECT_ID, USER_ID))
                .thenReturn(Optional.of(membership));
        return project;
    }

    private Response<ProjectStatus> execute(
            ProjectStatus currentStatus,
            ProjectStatus targetStatus
    ) {
        return switch (targetStatus) {
            case ACTIVE -> currentStatus == ProjectStatus.ON_HOLD
                    ? lifecycleService.resume(PROJECT_ID)
                    : lifecycleService.activate(PROJECT_ID);
            case ON_HOLD -> lifecycleService.hold(PROJECT_ID);
            case COMPLETED -> lifecycleService.complete(PROJECT_ID);
            case ARCHIVED -> lifecycleService.archive(PROJECT_ID);
            case PLANNING -> throw new IllegalArgumentException("No command transitions to PLANNING");
        };
    }

    private static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(ProjectStatus.PLANNING, ProjectStatus.ACTIVE, ProjectRole.MANAGER),
                Arguments.of(ProjectStatus.ACTIVE, ProjectStatus.ON_HOLD, ProjectRole.OWNER),
                Arguments.of(ProjectStatus.ON_HOLD, ProjectStatus.ACTIVE, ProjectRole.MANAGER),
                Arguments.of(ProjectStatus.ACTIVE, ProjectStatus.COMPLETED, ProjectRole.MANAGER),
                Arguments.of(ProjectStatus.COMPLETED, ProjectStatus.ARCHIVED, ProjectRole.OWNER)
        );
    }

    private static Specification<Task> anyTaskSpecification() {
        return org.mockito.ArgumentMatchers.<Specification<Task>>any();
    }

    private static Stream<Arguments> invalidTransitions() {
        return Stream.of(ProjectStatus.values())
                .flatMap(current -> Stream.of(
                        ProjectStatus.ACTIVE,
                        ProjectStatus.ON_HOLD,
                        ProjectStatus.COMPLETED,
                        ProjectStatus.ARCHIVED
                ).filter(target -> !isValid(current, target))
                 .map(target -> Arguments.of(current, target)));
    }

    private static boolean isValid(ProjectStatus current, ProjectStatus target) {
        return switch (current) {
            case PLANNING -> target == ProjectStatus.ACTIVE;
            case ACTIVE -> target == ProjectStatus.ON_HOLD || target == ProjectStatus.COMPLETED;
            case ON_HOLD -> target == ProjectStatus.ACTIVE;
            case COMPLETED -> target == ProjectStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
    }
}
