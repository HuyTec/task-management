package com.taskmanagement.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.page.PageResponse;
import com.taskmanagement.dto.project.CreateProjectRequest;
import com.taskmanagement.dto.task.TaskFilter;
import com.taskmanagement.dto.task.TaskResponse;
import com.taskmanagement.dto.task.CreateProjectTaskRequest;
import com.taskmanagement.dto.project.ProjectResponse;
import com.taskmanagement.dto.project.UpdateProjectRequest;
import com.taskmanagement.service.project.ProjectService;
import com.taskmanagement.service.task.TaskService;
import com.taskmanagement.service.task.TaskWorkflowService;
import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final TaskWorkflowService taskWorkflowService;
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<PageResponse<ProjectResponse>>> getAllProjects(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(projectService.getAllProjects(pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<Response<PageResponse<ProjectResponse>>> getMyProjects(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return ResponseEntity.ok(projectService.getMyProjects(pageable));
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
    public ResponseEntity<Response<PageResponse<TaskResponse>>> getProjectTasks(
            @PathVariable @Positive Long id,
            @Valid @ModelAttribute TaskFilter filter,
            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = "dueDate",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {
        return ResponseEntity.ok(taskService.getTasksByProject(id, filter, pageable));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<Response<TaskResponse>> createProjectTask(
            @PathVariable @Positive Long id,
            @RequestBody @Valid CreateProjectTaskRequest request
    ) {
        return ResponseEntity.ok(taskWorkflowService.createProjectTask(id, request));
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
