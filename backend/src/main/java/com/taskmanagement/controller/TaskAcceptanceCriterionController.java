package com.taskmanagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.task.AcceptanceCriterionResponse;
import com.taskmanagement.dto.task.CreateAcceptanceCriterionRequest;
import com.taskmanagement.dto.task.UpdateAcceptanceCriterionRequest;
import com.taskmanagement.service.task.TaskWorkflowService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tasks/{taskId}/criteria")
@RequiredArgsConstructor
public class TaskAcceptanceCriterionController {

    private final TaskWorkflowService taskWorkflowService;

    @PostMapping
    public ResponseEntity<Response<AcceptanceCriterionResponse>> addCriterion(
            @PathVariable @Positive Long taskId,
            @RequestBody @Valid CreateAcceptanceCriterionRequest request
    ) {
        return ResponseEntity.ok(taskWorkflowService.addCriterion(taskId, request));
    }

    @PatchMapping("/{criterionId}")
    public ResponseEntity<Response<AcceptanceCriterionResponse>> updateCriterion(
            @PathVariable @Positive Long taskId,
            @PathVariable @Positive Long criterionId,
            @RequestBody @Valid UpdateAcceptanceCriterionRequest request
    ) {
        return ResponseEntity.ok(taskWorkflowService.updateCriterion(taskId, criterionId, request));
    }

    @DeleteMapping("/{criterionId}")
    public ResponseEntity<Response<Void>> deleteCriterion(
            @PathVariable @Positive Long taskId,
            @PathVariable @Positive Long criterionId
    ) {
        return ResponseEntity.ok(taskWorkflowService.deleteCriterion(taskId, criterionId));
    }
}
