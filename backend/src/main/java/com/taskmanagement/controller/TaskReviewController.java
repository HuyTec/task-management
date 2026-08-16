package com.taskmanagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.task.RequestChangesRequest;
import com.taskmanagement.dto.task.TaskReviewResponse;
import com.taskmanagement.dto.task.TaskWorkflowResponse;
import com.taskmanagement.service.task.TaskWorkflowService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tasks/{taskId}")
@RequiredArgsConstructor
public class TaskReviewController {

    private final TaskWorkflowService taskWorkflowService;

    @PostMapping("/start")
    public ResponseEntity<Response<TaskWorkflowResponse>> start(
            @PathVariable @Positive Long taskId
    ) {
        return ResponseEntity.ok(taskWorkflowService.start(taskId));
    }

    @PostMapping("/submit-review")
    public ResponseEntity<Response<TaskWorkflowResponse>> submitReview(
            @PathVariable @Positive Long taskId
    ) {
        return ResponseEntity.ok(taskWorkflowService.submitReview(taskId));
    }

    @PostMapping("/request-changes")
    public ResponseEntity<Response<TaskReviewResponse>> requestChanges(
            @PathVariable @Positive Long taskId,
            @RequestBody @Valid RequestChangesRequest request
    ) {
        return ResponseEntity.ok(taskWorkflowService.requestChanges(taskId, request));
    }

    @PostMapping("/approve")
    public ResponseEntity<Response<TaskReviewResponse>> approve(
            @PathVariable @Positive Long taskId
    ) {
        return ResponseEntity.ok(taskWorkflowService.approve(taskId));
    }
}
