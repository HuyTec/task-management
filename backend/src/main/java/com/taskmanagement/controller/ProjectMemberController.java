package com.taskmanagement.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.taskmanagement.service.project.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.page.PageResponse;
import com.taskmanagement.dto.project.AddProjectMemberRequest;
import com.taskmanagement.dto.project.ProjectMemberResponse;
import com.taskmanagement.dto.project.UpdateProjectMemberRoleRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;


@RestController
@RequestMapping("/api/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<Response<PageResponse<ProjectMemberResponse>>> getProjectMembers(
            @PathVariable @Positive Long projectId,
            @RequestParam(required = false) @Size(max = 100) String search,
            @PageableDefault(size = 20, sort = "joinedAt", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(memberService.getMembers(projectId, search, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<Response<ProjectMemberResponse>> getMyProjectMembership(
            @PathVariable @Positive Long projectId
    ) {
        return ResponseEntity.ok(memberService.getMyMembership(projectId));
    }

    @PostMapping
    public ResponseEntity<Response<ProjectMemberResponse>> addProjectMember(
            @PathVariable @Positive Long projectId,
            @RequestBody @Valid AddProjectMemberRequest request
    ) {
        return ResponseEntity.ok(memberService.addMember(projectId, request));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Response<Void>> removeProjectMember(
            @PathVariable @Positive Long projectId,
            @PathVariable String username
    ) {
        return ResponseEntity.ok(memberService.removeMember(projectId, username));
    }

    @PatchMapping("/{username}/role")
    public ResponseEntity<Response<ProjectMemberResponse>> updateProjectMemberRole(
            @PathVariable @Positive Long projectId,
            @PathVariable String username,
            @RequestBody @Valid UpdateProjectMemberRoleRequest request
    ) {
        return ResponseEntity.ok(memberService.updateRole(projectId, username, request));
    }
}
