package com.taskmanagement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.submission.CreateLinkEvidenceRequest;
import com.taskmanagement.dto.submission.EvidenceResponse;
import com.taskmanagement.dto.submission.CompleteFileEvidenceRequest;
import com.taskmanagement.dto.submission.FileUploadInitiatedResponse;
import com.taskmanagement.dto.submission.InitiateFileEvidenceRequest;
import com.taskmanagement.dto.submission.SubmissionResponse;
import com.taskmanagement.service.submission.SubmissionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/submissions/{submissionId}")
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;

    @GetMapping
    public ResponseEntity<Response<SubmissionResponse>> get(
            @PathVariable @Positive Long submissionId
    ) {
        return ResponseEntity.ok(submissionService.get(submissionId));
    }

    @PostMapping("/evidences/links")
    public ResponseEntity<Response<EvidenceResponse>> addLink(
            @PathVariable @Positive Long submissionId,
            @RequestBody @Valid CreateLinkEvidenceRequest request
    ) {
        return ResponseEntity.ok(submissionService.addLink(submissionId, request));
    }

    @DeleteMapping("/evidences/{evidenceId}")
    public ResponseEntity<Response<Void>> deleteEvidence(
            @PathVariable @Positive Long submissionId,
            @PathVariable @Positive Long evidenceId
    ) {
        return ResponseEntity.ok(submissionService.deleteEvidence(submissionId, evidenceId));
    }

    @PostMapping("/evidences/files/initiate")
    public ResponseEntity<Response<FileUploadInitiatedResponse>> initiateFile(
            @PathVariable @Positive Long submissionId,
            @RequestBody @Valid InitiateFileEvidenceRequest request
    ) {
        return ResponseEntity.ok(submissionService.initiateFile(submissionId, request));
    }

    @PostMapping("/evidences/files/complete")
    public ResponseEntity<Response<EvidenceResponse>> completeFile(
            @PathVariable @Positive Long submissionId,
            @RequestBody @Valid CompleteFileEvidenceRequest request
    ) {
        return ResponseEntity.ok(submissionService.completeFile(submissionId, request));
    }

    @PostMapping("/submit")
    public ResponseEntity<Response<SubmissionResponse>> submit(
            @PathVariable @Positive Long submissionId
    ) {
        return ResponseEntity.ok(submissionService.submit(submissionId));
    }
}
