package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskmanagement.dto.task.CreateTaskRequest;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.mapper.ExpenseMapper;
import com.taskmanagement.mapper.TaskMapper;
import com.taskmanagement.model.Task;
import com.taskmanagement.repository.ExpenseRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.service.cache.ExpenseCacheService;
import com.taskmanagement.service.cache.TaskCacheService;
import com.taskmanagement.service.task.TaskService;
import com.taskmanagement.utils.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)   // (1)
class TaskServiceTest {

    // (2) — khai báo mock cho ĐỦ mọi dependency trong constructor của TaskService,
    // kể cả cái test này không đụng tới, để @InjectMocks ráp constructor không thiếu tham số
    @Mock private TaskCacheService taskCacheService;
    @Mock private ExpenseCacheService expenseCacheService;
    @Mock private UserRepository userRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private TaskMapper taskMapper;
    @Mock private ExpenseMapper expenseMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CustomUserDetails currentUser;

    @InjectMocks               // (3)
    private TaskService taskService;

    @Test
    void createTask_shouldThrowResourceNotFoundException_whenUserNotFound() {
        // Arrange (4) — chuẩn bị dữ liệu giả và "dạy" mock trả lời gì
        Long userId = 1L;
        CreateTaskRequest request = new CreateTaskRequest(/* điền đúng tham số thật của bạn */);

        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
        when(taskMapper.toTask(request)).thenReturn(new Task());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act + Assert (5) — gọi method thật, kiểm tra nó ném đúng exception
        assertThatThrownBy(() -> taskService.createTask(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("User not found!");
    }
}