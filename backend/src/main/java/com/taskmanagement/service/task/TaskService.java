package com.taskmanagement.service.task;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.expense.ExpenseResponse;
import com.taskmanagement.dto.task.CreateTaskRequest;
import com.taskmanagement.dto.task.TaskDetailResponse;
import com.taskmanagement.dto.task.TaskResponse;
import com.taskmanagement.dto.task.UpdateTaskRequest;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.ForbiddenException;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.mapper.ExpenseMapper;
import com.taskmanagement.mapper.TaskMapper;
import com.taskmanagement.model.Expense;
import com.taskmanagement.model.Task;
import com.taskmanagement.model.TaskStatus;
import com.taskmanagement.model.User;
import com.taskmanagement.repository.ExpenseRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.repository.projection.TaskTotalProjection;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.service.cache.ExpenseCacheService;
import com.taskmanagement.service.cache.TaskCacheService;
import com.taskmanagement.utils.SecurityUtils;

@Service
public class TaskService {
    private final TaskCacheService taskCacheService;
    private final ExpenseCacheService expenseCacheService;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ExpenseRepository expenseRepository;
    private final TaskMapper taskMapper;
    private final ExpenseMapper expenseMapper;
    private final SecurityUtils securityUtils;
    public TaskService(ExpenseCacheService expenseCacheService, TaskCacheService taskCacheService, UserRepository userRepository, TaskRepository taskRepository, ExpenseRepository expenseRepository, TaskMapper taskMapper, ExpenseMapper expenseMapper, SecurityUtils securityUtils){
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.expenseRepository = expenseRepository;
        this.taskMapper = taskMapper;
        this.expenseMapper = expenseMapper;
        this.securityUtils = securityUtils;
        this.taskCacheService = taskCacheService;
        this.expenseCacheService = expenseCacheService;
    }

    // private Task ensureTaskAvailable(boolean , Long userId, Long id) {
    //     return  ? taskRepository.findById(id)
    //                             .orElseThrow(() -> new ResourceNotFoundException("Task not found!"))
    //                     : taskRepository.findByIdAndUserId(id, userId)
    //                             .orElseThrow(() -> new ResourceNotFoundException("Task not found!"));
    // }

    private Task ensureTaskAvailable(Long userId, Long taskId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found!"));
    }

    public Response<TaskDetailResponse> getTaskById(Long id) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();

        Optional<TaskDetailResponse> cached = taskCacheService.get(id);
        if(cached.isPresent()){
            return Response.success(cached.get(),"Task data retrieved successfully!");
        }

        Task task = ensureTaskAvailable(currentUser.getId(), id);

        List<Expense> expenses = expenseRepository.findByTaskId(task.getId());
        List<ExpenseResponse> expenseResponses = expenses.stream().map(expenseMapper::toExpenseResponse).toList();
        Double total = expenses.stream().mapToDouble(Expense::getAmount).sum();
        TaskDetailResponse response = taskMapper.toTaskDetailResponse(task, expenseResponses, total);

        taskCacheService.put(response);

        return Response.success(response, "Task data retrieved successfully!");
    }

    public Response<List<TaskResponse>> getAllTask() {
        boolean isAdmin = securityUtils.isAdmin(securityUtils.getAuthentication());
        if(!isAdmin) 
            throw new ForbiddenException("Only admin can view all tasks");

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

    public Response<List<TaskResponse>> getMyTask() {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();

        List<Task> tasks = taskRepository.findByUserId(currentUser.getId());
        List<Long> taskIds = tasks.stream().map(Task::getId).toList();

        if(taskIds.isEmpty()){
            return Response.success(new ArrayList<>(), "Task data retrieved successfully!");
        }
        List<TaskTotalProjection> totals = expenseRepository.sumAmountsByTaskIds(taskIds);
        Map<Long, Double> totalMap = totals.stream()

            .collect(Collectors.toMap(TaskTotalProjection::getTaskId, TaskTotalProjection::getTotal));

        List<TaskResponse> responses = tasks.stream().map(task -> taskMapper.toTaskResponse(task, totalMap.getOrDefault(task.getId(), 0.0))).toList();

        return Response.success(responses, "Task data retrieved successfully!");
    }

    public Response<TaskResponse> createTask(CreateTaskRequest request){
        CustomUserDetails currentUser = securityUtils.getCurrentUser();


        Task task = taskMapper.toTask(request);
        User user = userRepository.findById(currentUser.getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        task.setUser(user);
        task.setStatus(TaskStatus.TODO);
        taskRepository.save(task);

        TaskResponse response = taskMapper.toTaskResponse(task, 0.0);
        return Response.success(response, "Task created successfully!");
    }

    public Response<TaskDetailResponse> updateTask(Long id, UpdateTaskRequest request) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        //Không bao giờ dùng Cached để update 
        
        Task task = ensureTaskAvailable(currentUser.getId(), id);

        if (request.title() != null) { 
            if (request.title().isBlank()) {
                throw new BadRequestException("Title cannot be blank");
            }
            task.setTitle(request.title());
        }

        if (request.description() != null) {
            task.setDescription(request.description());
        }

        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        
        if (request.status() != null) {
            task.setStatus(request.status());
        }

        if (request.dueDate() != null){
            task.setDueDate(request.dueDate());
        }

        taskRepository.save(task);
        taskCacheService.evict(id); //Xóa cache
        
        List<Expense> expenses = expenseRepository.findByTaskId(task.getId());
        List<ExpenseResponse> expenseResponses = expenses.stream().map(expenseMapper::toExpenseResponse).toList();
        Double total = expenses.stream().mapToDouble(Expense::getAmount).sum();

        TaskDetailResponse response = taskMapper.toTaskDetailResponse(task, expenseResponses, total);

        //Không cần put(response) vì lần sau sẽ tự động nạp lại

        return Response.success(response, "Task updated successfully!");
    }

    public Response<Void> deleteTaskById(Long id) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();

        Task task = ensureTaskAvailable(currentUser.getId(), id);

        List<Expense> expenses = expenseRepository.findByTaskId(task.getId());
        List<Long> expenseIds = expenses.stream().map(Expense::getId).toList();

        expenses.forEach(ex -> ex.setTask(null));
        expenseRepository.saveAll(expenses);

        taskRepository.delete(task);

        // Evict SAU KHI DB đã thay đổi thành công
        taskCacheService.evict(id);
        expenseIds.forEach(expenseCacheService::evict);

        return Response.success(null, "Task deleted successfully!");
    }
}
