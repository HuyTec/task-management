package com.taskmanagement.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.expense.CreateExpenseRequest;
import com.taskmanagement.dto.expense.ExpenseResponse;
import com.taskmanagement.dto.expense.UpdateExpenseRequest;
import com.taskmanagement.service.expense.ExpenseService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping("/me")
    public ResponseEntity<Response<List<ExpenseResponse>>> getMyExpenses() {
        return ResponseEntity.ok(expenseService.getMyExpenses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<ExpenseResponse>> getExpenseById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    @PostMapping
    public ResponseEntity<Response<ExpenseResponse>> createExpense(@RequestBody @Valid CreateExpenseRequest request) {
        return ResponseEntity.ok(expenseService.createExpense(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Response<ExpenseResponse>> updateExpense(@PathVariable @Positive Long id, @RequestBody @Valid UpdateExpenseRequest request) {
        return ResponseEntity.ok(expenseService.updateExpense(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> deleteExpense(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(expenseService.deleteExpense(id));
    }

    @PatchMapping("/{id}/task")
    public ResponseEntity<Response<ExpenseResponse>> unlinkTask(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(expenseService.unlinkTask(id));
    }
}