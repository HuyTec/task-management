package com.taskmanagement.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import java.util.List;

import com.taskmanagement.service.task.TaskService;
import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.task.TaskResponse;
import com.taskmanagement.dto.task.TaskDetailResponse;
import com.taskmanagement.dto.task.CreateTaskRequest;
import com.taskmanagement.dto.task.UpdateTaskRequest;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    // ADMIN ONLY — xem toàn bộ task trong hệ thống
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<List<TaskResponse>>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTask());
    }

    // SELF — user xem task của chính mình (Dashboard)
    @GetMapping("/me")
    public ResponseEntity<Response<List<TaskResponse>>> getMyTasks() {
        return ResponseEntity.ok(taskService.getMyTask());
    }

    // SELF/ADMIN — ownership check nằm trong Service (ensureTaskAvailable)
    @GetMapping("/{id}")
    public ResponseEntity<Response<TaskDetailResponse>> getTaskById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @PostMapping
    public ResponseEntity<Response<TaskResponse>> createTask(@RequestBody @Valid CreateTaskRequest request) {
        return ResponseEntity.ok(taskService.createTask(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Response<TaskDetailResponse>> updateTask(@PathVariable @Positive Long id, @RequestBody @Valid UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> deleteTask(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(taskService.deleteTaskById(id));
    }
}