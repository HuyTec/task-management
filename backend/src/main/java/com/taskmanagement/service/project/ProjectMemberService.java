package com.taskmanagement.service.project;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.project.AddProjectMemberRequest;
import com.taskmanagement.dto.project.ProjectMemberResponse;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.DuplicatedResourceException;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.model.Project;
import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.User;
import com.taskmanagement.repository.ProjectMemberRepository;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Response<List<ProjectMemberResponse>> getMembers(Long projectId) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        requireOwnedProject(projectId, currentUser.getId());

        List<ProjectMemberResponse> members = projectMemberRepository.findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
        return Response.success(members, "Project members retrieved successfully!");
    }

    public Response<ProjectMemberResponse> addMember(Long projectId, AddProjectMemberRequest request) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        Project project = requireOwnedProject(projectId, currentUser.getId());
        validateAssignableRole(request.role());

        User user = userRepository.findByUsernameAndIsDeactivatedFalse(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("Active user not found!"));
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new DuplicatedResourceException("User is already a project member");
        }

        ProjectMember membership = new ProjectMember();
        membership.setProject(project);
        membership.setUser(user);
        membership.setRole(request.role());
        ProjectMember savedMembership = projectMemberRepository.save(membership);

        return Response.success(toResponse(savedMembership), "Project member added successfully!");
    }

    public Response<Void> removeMember(Long projectId, String username) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        requireOwnedProject(projectId, currentUser.getId());

        ProjectMember membership = projectMemberRepository.findByProjectIdAndUserUsername(projectId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Project member not found!"));
        if (membership.getRole() == ProjectRole.OWNER) {
            throw new BadRequestException("Project owner cannot be removed");
        }

        projectMemberRepository.delete(membership);
        return Response.success(null, "Project member removed successfully!");
    }

    private Project requireOwnedProject(Long projectId, Long userId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found!"));
    }

    private void validateAssignableRole(ProjectRole role) {
        if (role != ProjectRole.MEMBER && role != ProjectRole.VIEWER) {
            throw new BadRequestException("Only MEMBER or VIEWER can be assigned in this phase");
        }
    }

    private ProjectMemberResponse toResponse(ProjectMember membership) {
        User user = membership.getUser();
        return new ProjectMemberResponse(
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                membership.getRole(),
                membership.getJoinedAt()
        );
    }
}
