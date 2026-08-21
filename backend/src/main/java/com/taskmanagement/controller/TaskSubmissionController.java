package com.taskmanagement.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.submission.SubmissionResponse;
import com.taskmanagement.service.submission.SubmissionService;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tasks/{taskId}/submissions")
@RequiredArgsConstructor
public class TaskSubmissionController {
    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<Response<SubmissionResponse>> create(
            @PathVariable @Positive Long taskId
    ) {
        return ResponseEntity.ok(submissionService.create(taskId));
    }

    @GetMapping
    public ResponseEntity<Response<List<SubmissionResponse>>> list(
            @PathVariable @Positive Long taskId
    ) {
        return ResponseEntity.ok(submissionService.listForTask(taskId));
    }
}
