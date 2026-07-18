package com.taskmanagement.service.expense;
import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.expense.CreateExpenseRequest;
import com.taskmanagement.dto.expense.ExpenseResponse;
import com.taskmanagement.dto.expense.UpdateExpenseRequest;
import com.taskmanagement.event.ExpenseCacheEvictEvent;
import com.taskmanagement.event.TaskCacheEvictEvent;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.mapper.ExpenseMapper;
import com.taskmanagement.model.Expense;
import com.taskmanagement.model.Task;
import com.taskmanagement.model.User;
import com.taskmanagement.repository.ExpenseRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.service.cache.ExpenseCacheService;
import com.taskmanagement.service.cache.TaskCacheService;
import com.taskmanagement.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final ExpenseMapper expenseMapper;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final SecurityUtils securityUtils;
    private final TaskCacheService taskCacheService;
    private final ExpenseCacheService expenseCacheService;
    private final ApplicationEventPublisher eventPublisher;

    private Expense ensureExpenseAvailable(Long userId, Long id) {
        return expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found!"));
    }

    private Task ensureTaskAvailable(Long userId, Long taskId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found!"));
    }

    public Response<ExpenseResponse> createExpense(CreateExpenseRequest request) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();

        Expense expense = expenseMapper.toExpense(request);

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        expense.setUser(user);

        if (request.taskId() != null) {
            Task task = ensureTaskAvailable(currentUser.getId(), request.taskId());
            expense.setTask(task);
        }

        expenseRepository.save(expense);

        if (expense.getTask() != null) {
            taskCacheService.evict(expense.getTask().getId());
        }

        ExpenseResponse response = expenseMapper.toExpenseResponse(expense);
        return Response.success(response, "Expense created successfully!");
    }

    @Transactional(readOnly = true)
    public Response<List<ExpenseResponse>> getMyExpenses() {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();

        List<Expense> expenses = expenseRepository.findByUserId(currentUser.getId());
        List<ExpenseResponse> responses = expenses.stream().map(expenseMapper::toExpenseResponse).toList();
        return Response.success(responses, "All expenses retrieved successfully!");
    }

    @Transactional(readOnly = true)
    public Response<ExpenseResponse> getExpenseById(Long id) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();

        Optional<ExpenseResponse> cached = expenseCacheService.get(id);

        if(cached.isPresent()){
            return Response.success(cached.get(), "Expense data retrieved successfully!");
        }


        Expense expense = ensureExpenseAvailable(currentUser.getId(), id);
        ExpenseResponse response = expenseMapper.toExpenseResponse(expense);

        expenseCacheService.put(response);

        return Response.success(response, "Expense data retrieved successfully!");
    }

    public Response<ExpenseResponse> updateExpense(Long id, UpdateExpenseRequest request) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        Expense expense = ensureExpenseAvailable(currentUser.getId(), id);

        if (expense.getTask() != null && request.taskId() != null) throw new BadRequestException("You have to unlink from other tasks");

        if (request.description() != null) {
            if (request.description().isBlank()) {
                throw new BadRequestException("Description cannot be blank");
            }
            expense.setDescription(request.description());
        }

        if (request.amount() != null) {
            if (request.amount() <= 0) {
                throw new BadRequestException("Amount must be positive");
            }
            expense.setAmount(request.amount());
        }

        if (request.category() != null) {
            expense.setCategory(request.category());
        }

        if (request.expenseDate() != null) {
            expense.setExpenseDate(request.expenseDate());
        }

        if (expense.getTask() == null && request.taskId() != null){
            Task newTask = ensureTaskAvailable(currentUser.getId(), request.taskId());
            expense.setTask(newTask);
        }

        expenseRepository.save(expense);
        eventPublisher.publishEvent(new ExpenseCacheEvictEvent(id));

        if (expense.getTask() != null) {
            eventPublisher.publishEvent(new TaskCacheEvictEvent(expense.getTask().getId()));
        }
        return Response.success(expenseMapper.toExpenseResponse(expense), "Expense updated successfully!");
    }

    public Response<Void> deleteExpense(Long id) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        Expense expense = ensureExpenseAvailable(currentUser.getId(), id);
        
        Task task = expense.getTask();
        Long taskId = (task != null) ? task.getId() : null;

        expenseRepository.delete(expense);
        eventPublisher.publishEvent(new ExpenseCacheEvictEvent(id));
        if (taskId != null) {
            eventPublisher.publishEvent(new TaskCacheEvictEvent(taskId));
        }

        return Response.success(null, "Expense deleted successfully!");
    }

    public Response<ExpenseResponse> unlinkTask(Long id){
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        Expense expense = ensureExpenseAvailable(currentUser.getId(), id);
        if(expense.getTask()==null) throw new ResourceNotFoundException("Expense already not linked to any task!");

        Long taskId = expense.getTask().getId();

        expense.setTask(null);
        ExpenseResponse response = expenseMapper.toExpenseResponse(expense);
        expenseRepository.save(expense);

        eventPublisher.publishEvent(new ExpenseCacheEvictEvent(id));
        eventPublisher.publishEvent(new TaskCacheEvictEvent(taskId));
        
        return Response.success(response, "Expense unlinked successfully!");
    }
}