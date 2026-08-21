package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.page.PageResponse;
import com.taskmanagement.dto.task.TaskFilter;
import com.taskmanagement.dto.task.TaskResponse;
import com.taskmanagement.dto.task.UpdateTaskRequest;
import com.taskmanagement.dto.task.CreateTaskRequest;
import com.taskmanagement.dto.task.TaskDetailResponse;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.ForbiddenException;
import com.taskmanagement.event.TaskCacheEvictEvent;
import com.taskmanagement.mapper.ExpenseMapper;
import com.taskmanagement.mapper.TaskMapper;
import com.taskmanagement.mapper.TaskWorkflowMapper;
import com.taskmanagement.model.Project;
import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.TaskStatus;
import com.taskmanagement.model.User;
import com.taskmanagement.model.Task;
import com.taskmanagement.repository.ExpenseRepository;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.MemberRepository;
import com.taskmanagement.repository.TaskAcceptanceCriterionRepository;
import com.taskmanagement.repository.TaskAssignmentRepository;
import com.taskmanagement.repository.TaskReviewRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.service.cache.TaskCacheService;
import com.taskmanagement.service.task.TaskService;
import com.taskmanagement.utils.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskCacheService taskCacheService;
    @Mock private UserRepository userRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private TaskMapper taskMapper;
    @Mock private ExpenseMapper expenseMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private ProjectRepository projectRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private TaskAssignmentRepository assignmentRepository;
    @Mock private TaskAcceptanceCriterionRepository criterionRepository;
    @Mock private TaskReviewRepository reviewRepository;
    @Mock private TaskWorkflowMapper workflowMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CustomUserDetails currentUser;

    @InjectMocks
    private TaskService taskService;

    @Test
    void getMyTasksReturnsAnEmptyPageWithoutQueryingExpenseTotals() {
        Long userId = 7L;
        PageRequest pageable = PageRequest.of(0, 20);
        TaskFilter filter = new TaskFilter(null, null, null, null, null, null);

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        when(taskRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Task>>any(), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        Response<PageResponse<TaskResponse>> response = taskService.getMyTask(filter, pageable);

        assertThat(response.data().content()).isEmpty();
        assertThat(response.data().first()).isTrue();
        assertThat(response.data().last()).isTrue();
        verify(expenseRepository, never()).sumAmountsByTaskIds(any());
    }

    @Test
    void createTaskShouldFailWhenCurrentUserNoLongerExists() {
        Long userId = 1L;
        CreateTaskRequest request = new CreateTaskRequest("Test task", null, null, null,null);

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        when(taskMapper.toTask(request)).thenReturn(new Task());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found!");
    }

    @Test
    void getTaskByIdUsesCurrentUserInCacheKey() {
        Long userId = 7L;
        Long taskId = 42L;
        TaskDetailResponse cachedTask = new TaskDetailResponse(
                taskId, "Private task", null, null, null, userId,
                null,null,  null, null, List.of(), 0.0);

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        Task readableTask = new Task();
        User owner = new User();
        owner.setId(userId);
        readableTask.setUser(owner);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(readableTask));
        when(taskCacheService.get(userId, taskId)).thenReturn(Optional.of(cachedTask));

        Response<TaskDetailResponse> response = taskService.getTaskById(taskId);

        assertThat(response.data()).isEqualTo(cachedTask);
        verify(taskCacheService).get(userId, taskId);
        verify(taskRepository).findById(taskId);
        verify(taskRepository, never()).findByIdAndUserId(taskId, userId);
    }

    @Test
    void createTaskRejectsProjectIdBecauseProjectTasksUseAtomicEndpoint() {
        Long projectId = 12L;
        CreateTaskRequest request = new CreateTaskRequest(
                "Project task", null, null, null, projectId
        );

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Project tasks must be created through the project task endpoint");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void projectIdIsRejectedBeforeAnyProjectLookup() {
        Long projectId = 12L;
        CreateTaskRequest request = new CreateTaskRequest(
                "Foreign project task", null, null, null, projectId
        );

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(BadRequestException.class);

        verify(projectRepository, never()).findAccessibleProject(any(), any());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTaskRejectsProjectRelationChanges() {
        Long userId = 7L;
        Long taskId = 42L;
        Project project = new Project();
        project.setId(12L);
        Task task = new Task();
        task.setId(taskId);
        task.setTitle("Keep this title");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setProject(project);
        UpdateTaskRequest request = new UpdateTaskRequest(
                null, null, null, null, null, 0L
        );

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(memberRepository.findByProjectIdAndUserId(12L, userId))
                .thenReturn(Optional.of(membership(project, new User(), ProjectRole.MANAGER)));

        assertThatThrownBy(() -> taskService.updateTask(taskId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Task project relation cannot change through the generic update endpoint");

        assertThat(task.getProject()).isSameAs(project);
        verify(taskRepository, never()).save(task);
        verifyNoInteractions(projectRepository);
    }

    @Test
    void unlinkFromProjectKeepsTaskAndEvictsItsCache() {
        Long userId = 7L;
        Long taskId = 42L;
        Task task = new Task();
        Project project = new Project();
        project.setId(12L);
        task.setProject(project);

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(memberRepository.findByProjectIdAndUserId(12L, userId))
                .thenReturn(Optional.of(membership(project, new User(), ProjectRole.OWNER)));

        taskService.unlinkFromProject(taskId);

        assertThat(task.getProject()).isNull();
        verify(taskRepository).save(task);
        verify(eventPublisher).publishEvent(
                new com.taskmanagement.event.TaskCacheEvictEvent(userId, taskId)
        );
        verify(taskRepository, never()).delete(task);
    }

    @Test
    void updateTaskRejectsTargetProjectBeforeTargetProjectLookup() {
        Long userId = 7L;
        Long taskId = 42L;
        Project sourceProject = new Project();
        sourceProject.setId(12L);
        Project targetProject = new Project();
        targetProject.setId(13L);
        Task task = new Task();
        task.setId(taskId);
        task.setProject(sourceProject);
        UpdateTaskRequest request = new UpdateTaskRequest(
                null, null, null, null, null, targetProject.getId()
        );

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(memberRepository.findByProjectIdAndUserId(sourceProject.getId(), userId))
                .thenReturn(Optional.of(membership(sourceProject, user(userId), ProjectRole.MANAGER)));
        assertThatThrownBy(() -> taskService.updateTask(taskId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Task project relation cannot change through the generic update endpoint");

        assertThat(task.getProject()).isSameAs(sourceProject);
        verifyNoInteractions(projectRepository);
        verify(taskRepository, never()).save(task);
    }

    @Test
    void updateProjectTaskEvictsCacheForEveryProjectMember() {
        Long actorUserId = 7L;
        Long otherUserId = 9L;
        Long taskId = 42L;
        Project project = new Project();
        project.setId(12L);
        User creator = user(actorUserId);
        Task task = new Task();
        task.setId(taskId);
        task.setUser(creator);
        task.setProject(project);
        task.setStatus(TaskStatus.TODO);
        UpdateTaskRequest request = new UpdateTaskRequest(
                "Updated title", null, null, null, null, null
        );

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(actorUserId);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(memberRepository.findByProjectIdAndUserId(project.getId(), actorUserId))
                .thenReturn(Optional.of(membership(project, creator, ProjectRole.MANAGER)));
        when(memberRepository.findByProjectId(project.getId())).thenReturn(List.of(
                membership(project, creator, ProjectRole.MANAGER),
                membership(project, user(otherUserId), ProjectRole.MEMBER)
        ));

        taskService.updateTask(taskId, request);

        verify(eventPublisher).publishEvent(new TaskCacheEvictEvent(actorUserId, taskId));
        verify(eventPublisher).publishEvent(new TaskCacheEvictEvent(otherUserId, taskId));
    }

    private ProjectMember membership(Project project, User user, ProjectRole role) {
        ProjectMember membership = new ProjectMember();
        membership.setProject(project);
        membership.setUser(user);
        membership.setRole(role);
        return membership;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
