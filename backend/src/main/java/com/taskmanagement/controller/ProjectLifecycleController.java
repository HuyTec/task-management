package com.taskmanagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskmanagement.dto.Response;
import com.taskmanagement.model.ProjectStatus;
import com.taskmanagement.service.ProjectLifecycleService;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class ProjectLifecycleController {

    private final ProjectLifecycleService lifecycleService;

    @PostMapping("/activate")
    public ResponseEntity<Response<ProjectStatus>> activate(
            @PathVariable @Positive Long projectId
    ) {
        return ResponseEntity.ok(lifecycleService.activate(projectId));
    }

    @PostMapping("/hold")
    public ResponseEntity<Response<ProjectStatus>> hold(
            @PathVariable @Positive Long projectId
    ) {
        return ResponseEntity.ok(lifecycleService.hold(projectId));
    }

    @PostMapping("/resume")
    public ResponseEntity<Response<ProjectStatus>> resume(
            @PathVariable @Positive Long projectId
    ) {
        return ResponseEntity.ok(lifecycleService.resume(projectId));
    }

    @PostMapping("/complete")
    public ResponseEntity<Response<ProjectStatus>> complete(
            @PathVariable @Positive Long projectId
    ) {
        return ResponseEntity.ok(lifecycleService.complete(projectId));
    }

    @PostMapping("/archive")
    public ResponseEntity<Response<ProjectStatus>> archive(
            @PathVariable @Positive Long projectId
    ) {
        return ResponseEntity.ok(lifecycleService.archive(projectId));
    }
}
