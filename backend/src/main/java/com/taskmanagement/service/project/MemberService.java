package com.taskmanagement.service.project;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.page.PageResponse;
import com.taskmanagement.dto.project.AddProjectMemberRequest;
import com.taskmanagement.dto.project.ProjectMemberResponse;
import com.taskmanagement.dto.project.UpdateProjectMemberRoleRequest;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.DuplicatedResourceException;
import com.taskmanagement.exception.ForbiddenException;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.AssignmentStatus;
import com.taskmanagement.model.User;
import com.taskmanagement.repository.MemberRepository;
import com.taskmanagement.repository.TaskAssignmentRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.utils.SecurityUtils;
import com.taskmanagement.utils.PageableValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberService {
    private static final Set<String> ALLOWED_SORTS = Set.of("joinedAt", "role");

    private final MemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Response<PageResponse<ProjectMemberResponse>> getMembers(
            Long projectId,
            String search,
        Pageable pageable
    ) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        requireMembership(projectId, currentUser.getId());
        PageableValidator.requireAllowedSorts(pageable, ALLOWED_SORTS);

        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        Page<ProjectMember> memberPage = normalizedSearch.isEmpty()
                ? projectMemberRepository.findByProjectId(projectId, pageable)
                : projectMemberRepository.findByProjectIdAndSearch(projectId, normalizedSearch, pageable);
        List<ProjectMemberResponse> members = memberPage.getContent().stream()
                .map(this::toResponse)
                .toList();
        return Response.success(PageResponse.from(memberPage, members), "Project members retrieved successfully!");
    }

    @Transactional(readOnly = true)
    public Response<ProjectMemberResponse> getMyMembership(Long projectId) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        ProjectMember membership = requireMembership(projectId, currentUser.getId());
        return Response.success(toResponse(membership), "Project membership retrieved successfully!");
    }

    public Response<ProjectMemberResponse> addMember(Long projectId, AddProjectMemberRequest request) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        ProjectMember actor = requireManager(projectId, currentUser.getId());
        validateAssignableRole(actor, request.role());

        User user = userRepository.findByUsernameAndIsDeactivatedFalse(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("Active user not found!"));
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new DuplicatedResourceException("User is already a project member");
        }

        ProjectMember membership = new ProjectMember();
        membership.setProject(actor.getProject());
        membership.setUser(user);
        membership.setRole(request.role());
        ProjectMember savedMembership = projectMemberRepository.save(membership);

        return Response.success(toResponse(savedMembership), "Project member added successfully!");
    }

    public Response<Void> removeMember(Long projectId, String username) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        ProjectMember actor = requireManager(projectId, currentUser.getId());

        ProjectMember membership = projectMemberRepository.findByProjectIdAndUserUsername(projectId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Project member not found!"));
        if (membership.getRole() == ProjectRole.OWNER) {
            log.warn("Denied project owner removal: actorUserId={}, projectId={}, targetUserId={}",
                    currentUser.getId(), projectId, membership.getUser().getId());
            throw new BadRequestException("Project owner cannot be modified or removed");
        }
        ensureActorCanManage(actor, membership);
        if (taskAssignmentRepository.existsByAssigneeIdAndStatus(
                membership.getId(), AssignmentStatus.ACTIVE
        )) {
            throw new BadRequestException(
                    "Reassign active tasks before removing this project member"
            );
        }

        projectMemberRepository.delete(membership);
        return Response.success(null, "Project member removed successfully!");
    }

    public Response<ProjectMemberResponse> updateRole(
            Long projectId,
            String username,
            UpdateProjectMemberRoleRequest request
    ) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        ProjectMember actor = requireManager(projectId, currentUser.getId());
        ProjectMember membership = projectMemberRepository.findByProjectIdAndUserUsername(projectId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Project member not found!"));

        if (membership.getRole() == ProjectRole.OWNER) {
            log.warn("Denied project owner role change: actorUserId={}, projectId={}, targetUserId={}",
                    currentUser.getId(), projectId, membership.getUser().getId());
            throw new BadRequestException("Project owner role cannot be changed");
        }
        ensureActorCanManage(actor, membership);
        validateAssignableRole(actor, request.role());

        membership.setRole(request.role());
        ProjectMember savedMembership = projectMemberRepository.save(membership);
        return Response.success(toResponse(savedMembership), "Project member role updated successfully!");
    }

    private ProjectMember requireMembership(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found!"));
    }

    private ProjectMember requireManager(Long projectId, Long userId) {
        ProjectMember membership = requireMembership(projectId, userId);
        if (membership.getRole() != ProjectRole.OWNER && membership.getRole() != ProjectRole.MANAGER) {
            log.warn("Denied project member management: actorUserId={}, projectId={}, actorRole={}",
                    userId, projectId, membership.getRole());
            throw new ForbiddenException("Project member management requires OWNER or MANAGER role");
        }
        return membership;
    }

    private void ensureActorCanManage(ProjectMember actor, ProjectMember target) {
        if (actor.getRole() == ProjectRole.MANAGER && target.getRole() == ProjectRole.MANAGER) {
            log.warn("Denied manager management by peer manager: actorUserId={}, projectId={}, targetUserId={}",
                    actor.getUser().getId(), actor.getProject().getId(), target.getUser().getId());
            throw new ForbiddenException("Only project owner can manage project admins");
        }
    }

    private void validateAssignableRole(ProjectMember actor, ProjectRole targetRole) {
        if (targetRole == ProjectRole.OWNER) {
            log.warn("Denied OWNER role assignment: actorUserId={}, projectId={}, actorRole={}",
                    actor.getUser().getId(), actor.getProject().getId(), actor.getRole());
            throw new BadRequestException("Project owner role cannot be assigned");
        }
        if (targetRole == ProjectRole.MANAGER && actor.getRole() != ProjectRole.OWNER) {
            log.warn("Denied MANAGER role assignment: actorUserId={}, projectId={}, actorRole={}",
                    actor.getUser().getId(), actor.getProject().getId(), actor.getRole());
            throw new ForbiddenException("Only project owner can assign the manager role");
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
