package com.taskmanagement.service.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.task.CreateTaskRequest;
import com.taskmanagement.dto.task.TaskResponse;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.DuplicatedResourceException;
import com.taskmanagement.exception.ForbiddenException;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.mapper.TaskMapper;
import com.taskmanagement.model.Task;
import com.taskmanagement.repository.ExpenseRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.repository.projection.TaskTotalProjection;
import com.taskmanagement.security.CustomUserDetails;

@Service
public class TaskService {
    private final TaskResponse taskResponse;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ExpenseRepository expenseRepository;
    private final TaskMapper taskMapper;
    public TaskService(UserRepository userRepository, TaskRepository taskRepository, ExpenseRepository expenseRepository, TaskMapper taskMapper, TaskResponse taskResponse){
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.expenseRepository = expenseRepository;
        this.taskMapper = taskMapper;
        this.taskResponse = taskResponse;
    }

    private void ensureFieldNotBlank(String input) {
        if (input.isBlank() || input == null) {
            throw new BadRequestException("Field " + input + "is blank or null!");
        }
    }

    public Response<TaskResponse> getTaskById(Long id){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();
        
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        Task task = taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found!"));
        String username = task.getUser().getUsername();
        //TODO: check compare user id
        return null;
    }

    public Response<List<TaskResponse>> getAllTask() {
        List<Task> tasks =  taskRepository.findAll();
        List<TaskResponse> taskResponses = new ArrayList<>();
        List<Long> taskIds = tasks.stream().map(Task::getId).toList();

        if(taskIds.isEmpty()){
            return Response.success(new ArrayList<>(), "Task data retrieved successfully!");
        }
        List<TaskTotalProjection> totals = expenseRepository.sumAmountsByTaskIds(taskIds);

        Map<Long, Double> totalMap = totals.stream()
            .collect(Collectors.toMap(TaskTotalProjection::getTaskId, TaskTotalProjection::getTotal));

        for (Task task : tasks) {
            Double total = totalMap.getOrDefault(task.getId(), 0.0);
            TaskResponse response = taskMapper.toTaskResponse(task, total);
            taskResponses.add(response);
        }
        return Response.success(taskResponses, "Task data retrieved successfully!");
    }

    public Response<TaskResponse> createTask(CreateTaskRequest request){
        return null;
    }


}
