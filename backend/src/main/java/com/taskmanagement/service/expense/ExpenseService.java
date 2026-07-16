package com.taskmanagement.service.expense;

import java.util.List;

import org.springframework.stereotype.Service;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.expense.CreateExpenseRequest;
import com.taskmanagement.dto.expense.ExpenseResponse;
import com.taskmanagement.dto.expense.UpdateExpenseRequest;
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

    @Service
    public class ExpenseService {
        private final ExpenseRepository expenseRepository;
        private final ExpenseMapper expenseMapper;
        private final UserRepository userRepository;
        private final TaskRepository taskRepository;
        private final SecurityUtils securityUtils;
        private final TaskCacheService taskCacheService;
        private final ExpenseCacheService expenseCacheService;

        public ExpenseService(ExpenseCacheService expenseCacheService, TaskCacheService taskCacheService,ExpenseRepository expenseRepository, ExpenseMapper expenseMapper, SecurityUtils securityUtils, UserRepository userRepository, TaskRepository taskRepository) {
            this.expenseMapper = expenseMapper;
            this.expenseRepository = expenseRepository;
            this.securityUtils = securityUtils;
            this.userRepository = userRepository;
            this.taskRepository = taskRepository;
            this.taskCacheService = taskCacheService;
            this.expenseCacheService = expenseCacheService;
        }

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

            ExpenseResponse response = expenseMapper.toExpenseResponse(expense);
            return Response.success(response, "Expense created successfully!");
        }

        public Response<List<ExpenseResponse>> getMyExpenses() {
            CustomUserDetails currentUser = securityUtils.getCurrentUser();

            List<Expense> expenses = expenseRepository.findByUserId(currentUser.getId());
            List<ExpenseResponse> responses = expenses.stream().map(expenseMapper::toExpenseResponse).toList();
            return Response.success(responses, "All expenses retrieved successfully!");
        }

        public Response<ExpenseResponse> getExpenseById(Long id) {
            CustomUserDetails currentUser = securityUtils.getCurrentUser();
            Expense expense = ensureExpenseAvailable(currentUser.getId(), id);
            return Response.success(expenseMapper.toExpenseResponse(expense), "Expense data retrieved successfully!");
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
            return Response.success(expenseMapper.toExpenseResponse(expense), "Expense updated successfully!");
        }

        public Response<Void> deleteExpense(Long id) {
            CustomUserDetails currentUser = securityUtils.getCurrentUser();
            Expense expense = ensureExpenseAvailable(currentUser.getId(), id);
            expenseRepository.delete(expense);
            return Response.success(null, "Expense deleted successfully!");
        }

        public Response<ExpenseResponse> unlinkTask(Long id){
            CustomUserDetails currentUser = securityUtils.getCurrentUser();
            Expense expense = ensureExpenseAvailable(currentUser.getId(), id);
            if(expense.getTask()==null) throw new ResourceNotFoundException("Expense already not linked to any task!");
            expense.setTask(null);
            ExpenseResponse response = expenseMapper.toExpenseResponse(expense);
            expenseRepository.save(expense);
            return Response.success(response, "Expense unlinked successfully!");
        }
    }