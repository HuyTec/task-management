package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.task.CreateTaskRequest;
import com.taskmanagement.dto.task.TaskDetailResponse;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.mapper.ExpenseMapper;
import com.taskmanagement.mapper.TaskMapper;
import com.taskmanagement.model.Task;
import com.taskmanagement.repository.ExpenseRepository;
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
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CustomUserDetails currentUser;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTaskShouldFailWhenCurrentUserNoLongerExists() {
        Long userId = 1L;
        CreateTaskRequest request = new CreateTaskRequest("Test task", null, null, null);

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
                null, null, null, List.of(), 0.0);

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        when(taskCacheService.get(userId, taskId)).thenReturn(Optional.of(cachedTask));

        Response<TaskDetailResponse> response = taskService.getTaskById(taskId);

        assertThat(response.data()).isEqualTo(cachedTask);
        verify(taskCacheService).get(userId, taskId);
        verify(taskRepository, never()).findByIdAndUserId(taskId, userId);
    }
}
