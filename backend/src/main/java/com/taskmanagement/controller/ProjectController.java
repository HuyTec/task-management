package com.taskmanagement.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.project.CreateProjectRequest;
import com.taskmanagement.dto.task.TaskResponse;
import com.taskmanagement.dto.project.ProjectResponse;
import com.taskmanagement.dto.project.UpdateProjectRequest;
import com.taskmanagement.service.project.ProjectService;
import com.taskmanagement.service.task.TaskService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    private final TaskService taskService;
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<List<ProjectResponse>>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/me")
    public ResponseEntity<Response<List<ProjectResponse>>> getMyProjects() {
        return ResponseEntity.ok(projectService.getMyProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<ProjectResponse>> getProjectById(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @PostMapping
    public ResponseEntity<Response<ProjectResponse>> createProject(
            @RequestBody @Valid CreateProjectRequest request
    ) {

        return ResponseEntity.ok(projectService.createProject(request));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<Response<List<TaskResponse>>> getProjectTasks(
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(taskService.getTasksByProject(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Response<ProjectResponse>> updateProject(
            @PathVariable @Positive Long id,
            @RequestBody @Valid UpdateProjectRequest request
    ) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response<Void>> deleteProject(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(projectService.deleteProject(id));
    }
}
