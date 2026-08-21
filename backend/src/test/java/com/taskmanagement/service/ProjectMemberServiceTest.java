package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.page.PageResponse;
import com.taskmanagement.dto.project.AddProjectMemberRequest;
import com.taskmanagement.dto.project.ProjectMemberResponse;
import com.taskmanagement.dto.project.UpdateProjectMemberRoleRequest;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.DuplicatedResourceException;
import com.taskmanagement.exception.ForbiddenException;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.model.AssignmentStatus;
import com.taskmanagement.model.Project;
import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.User;
import com.taskmanagement.repository.MemberRepository;
import com.taskmanagement.repository.TaskAssignmentRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.service.project.MemberService;
import com.taskmanagement.utils.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock private MemberRepository projectMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskAssignmentRepository taskAssignmentRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private CustomUserDetails currentUser;

    @InjectMocks
    private MemberService projectMemberService;

    @Test
    void ownerCanListMembersWithoutExposingIds() {
        Long ownerId = 7L;
        Long projectId = 42L;
        Project project = new Project();
        User memberUser = user(9L, "lan", "Lan", "lan@example.com");
        ProjectMember membership = membership(project, memberUser, ProjectRole.MEMBER);
        membership.setJoinedAt(LocalDateTime.of(2026, 8, 14, 9, 30));
        PageRequest pageable = PageRequest.of(0, 20);
        stubOwner(projectId, ownerId, project);
        when(projectMemberRepository.findByProjectId(projectId, pageable))
                .thenReturn(new PageImpl<>(List.of(membership), pageable, 1));

        Response<PageResponse<ProjectMemberResponse>> response = projectMemberService.getMembers(projectId, null, pageable);

        assertThat(response.data().content()).containsExactly(new ProjectMemberResponse(
                "lan", "Lan", "lan@example.com", ProjectRole.MEMBER, membership.getJoinedAt()
        ));
        assertThat(response.data().totalElements()).isEqualTo(1);
    }

    @Test
    void viewerCanListMembersButReceivesNoMutationAuthorityFromThisRead() {
        Long viewerId = 7L;
        Long projectId = 42L;
        Project project = project(projectId);
        ProjectMember owner = membership(
                project,
                user(9L, "owner", "Owner", "owner@example.com"),
                ProjectRole.OWNER
        );
        PageRequest pageable = PageRequest.of(0, 20);
        stubActor(projectId, viewerId, project, ProjectRole.VIEWER);
        when(projectMemberRepository.findByProjectId(projectId, pageable))
                .thenReturn(new PageImpl<>(List.of(owner), pageable, 1));

        Response<PageResponse<ProjectMemberResponse>> response =
                projectMemberService.getMembers(projectId, null, pageable);

        assertThat(response.data().content()).singleElement()
                .extracting(ProjectMemberResponse::role)
                .isEqualTo(ProjectRole.OWNER);
        verifyNoInteractions(userRepository, taskAssignmentRepository);
    }

    @Test
    void addMemberRejectsDuplicateMembership() {
        Long ownerId = 7L;
        Long projectId = 42L;
        Project project = new Project();
        User memberUser = user(9L, "lan", "Lan", "lan@example.com");
        stubOwner(projectId, ownerId, project);
        when(userRepository.findByUsernameAndIsDeactivatedFalse("lan")).thenReturn(Optional.of(memberUser));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, memberUser.getId())).thenReturn(true);

        assertThatThrownBy(() -> projectMemberService.addMember(
                projectId, new AddProjectMemberRequest("lan", ProjectRole.MEMBER)
        )).isInstanceOf(DuplicatedResourceException.class);

        verify(projectMemberRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ownerCanAddManager() {
        Long ownerId = 7L;
        Long projectId = 42L;
        Project project = project(projectId);
        User manager = user(9L, "manager", "Manager", "manager@example.com");
        stubActor(projectId, ownerId, project, ProjectRole.OWNER);
        when(userRepository.findByUsernameAndIsDeactivatedFalse("manager"))
                .thenReturn(Optional.of(manager));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, manager.getId()))
                .thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Response<ProjectMemberResponse> response = projectMemberService.addMember(
                projectId,
                new AddProjectMemberRequest("manager", ProjectRole.MANAGER)
        );

        assertThat(response.data().username()).isEqualTo("manager");
        assertThat(response.data().role()).isEqualTo(ProjectRole.MANAGER);
        ArgumentCaptor<ProjectMember> savedMembership = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository).save(savedMembership.capture());
        assertThat(savedMembership.getValue().getProject()).isSameAs(project);
        assertThat(savedMembership.getValue().getUser()).isSameAs(manager);
    }

    @ParameterizedTest
    @EnumSource(value = ProjectRole.class, names = {"MEMBER", "VIEWER"})
    void managerCanAddRegularMember(ProjectRole targetRole) {
        Long managerId = 7L;
        Long projectId = 42L;
        Project project = project(projectId);
        User member = user(9L, "lan", "Lan", "lan@example.com");
        stubActor(projectId, managerId, project, ProjectRole.MANAGER);
        when(userRepository.findByUsernameAndIsDeactivatedFalse("lan"))
                .thenReturn(Optional.of(member));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, member.getId()))
                .thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Response<ProjectMemberResponse> response = projectMemberService.addMember(
                projectId,
                new AddProjectMemberRequest("lan", targetRole)
        );

        assertThat(response.data().role()).isEqualTo(targetRole);
    }

    @Test
    void managerCannotAddManagerAndTargetUserIsNotLookedUp() {
        Long managerId = 7L;
        Long projectId = 42L;
        stubActor(projectId, managerId, project(projectId), ProjectRole.MANAGER);

        assertThatThrownBy(() -> projectMemberService.addMember(
                projectId,
                new AddProjectMemberRequest("another-manager", ProjectRole.MANAGER)
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only project owner can assign the manager role");

        verifyNoInteractions(userRepository);
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void ownerRoleCannotBeAssignedAndTargetUserIsNotLookedUp() {
        Long ownerId = 7L;
        Long projectId = 42L;
        stubOwner(projectId, ownerId, project(projectId));

        assertThatThrownBy(() -> projectMemberService.addMember(
                projectId,
                new AddProjectMemberRequest("second-owner", ProjectRole.OWNER)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Project owner role cannot be assigned");

        verifyNoInteractions(userRepository);
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @ParameterizedTest
    @EnumSource(value = ProjectRole.class, names = {"MEMBER", "VIEWER"})
    void memberOrViewerCannotAddMemberAndTargetUserIsNotLookedUp(ProjectRole actorRole) {
        Long actorId = 7L;
        Long projectId = 42L;
        stubActor(projectId, actorId, project(projectId), actorRole);

        assertThatThrownBy(() -> projectMemberService.addMember(
                projectId,
                new AddProjectMemberRequest("lan", ProjectRole.MEMBER)
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Project member management requires OWNER or MANAGER role");

        verifyNoInteractions(userRepository);
        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void addMemberRejectsMissingOrDeactivatedUser() {
        Long ownerId = 7L;
        Long projectId = 42L;
        stubOwner(projectId, ownerId, project(projectId));
        when(userRepository.findByUsernameAndIsDeactivatedFalse("inactive"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectMemberService.addMember(
                projectId,
                new AddProjectMemberRequest("inactive", ProjectRole.MEMBER)
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Active user not found!");

        verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    void removeMemberProtectsProjectOwner() {
        Long ownerId = 7L;
        Long projectId = 42L;
        Project project = new Project();
        User owner = user(ownerId, "owner", "Owner", "owner@example.com");
        ProjectMember ownerMembership = membership(project, owner, ProjectRole.OWNER);
        stubOwner(projectId, ownerId, project);
        when(projectMemberRepository.findByProjectIdAndUserUsername(projectId, "owner"))
                .thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> projectMemberService.removeMember(projectId, "owner"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Project owner cannot be modified or removed");

        verify(projectMemberRepository, never()).delete(ownerMembership);
        verifyNoInteractions(userRepository);
    }

    @Test
    void managerCannotManagePeerManager() {
        Long managerId = 7L;
        Long projectId = 42L;
        Project project = project(projectId);
        ProjectMember peerManager = membership(
                project,
                user(9L, "peer", "Peer", "peer@example.com"),
                ProjectRole.MANAGER
        );
        stubActor(projectId, managerId, project, ProjectRole.MANAGER);
        when(projectMemberRepository.findByProjectIdAndUserUsername(projectId, "peer"))
                .thenReturn(Optional.of(peerManager));

        assertThatThrownBy(() -> projectMemberService.removeMember(projectId, "peer"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only project owner can manage project managers");

        verify(projectMemberRepository, never()).delete(peerManager);
    }

    @Test
    void updateRoleProtectsProjectOwner() {
        Long ownerId = 7L;
        Long projectId = 42L;
        Project project = project(projectId);
        ProjectMember owner = membership(
                project,
                user(ownerId, "owner", "Owner", "owner@example.com"),
                ProjectRole.OWNER
        );
        stubOwner(projectId, ownerId, project);
        when(projectMemberRepository.findByProjectIdAndUserUsername(projectId, "owner"))
                .thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> projectMemberService.updateRole(
                projectId,
                "owner",
                new UpdateProjectMemberRoleRequest(ProjectRole.MEMBER)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Project owner role cannot be changed");

        verify(projectMemberRepository, never()).save(owner);
    }

    @Test
    void updateRoleAllowsViewerDemotionWhenMemberHasNoActiveAssignment() {
        Long ownerId = 7L;
        Long projectId = 42L;
        Project project = project(projectId);
        ProjectMember member = membership(
                project,
                user(9L, "lan", "Lan", "lan@example.com"),
                ProjectRole.MEMBER
        );
        member.setId(17L);
        stubOwner(projectId, ownerId, project);
        when(projectMemberRepository.findByProjectIdAndUserUsername(projectId, "lan"))
                .thenReturn(Optional.of(member));
        when(taskAssignmentRepository.existsByAssigneeIdAndStatus(17L, AssignmentStatus.ACTIVE))
                .thenReturn(false);
        when(projectMemberRepository.save(member)).thenReturn(member);

        Response<ProjectMemberResponse> response = projectMemberService.updateRole(
                projectId,
                "lan",
                new UpdateProjectMemberRoleRequest(ProjectRole.VIEWER)
        );

        assertThat(response.data().role()).isEqualTo(ProjectRole.VIEWER);
    }

    @Test
    void removeMemberRejectsActiveAssignee() {
        Long ownerId = 7L;
        Long projectId = 42L;
        Project project = project(projectId);
        ProjectMember member = membership(
                project,
                user(9L, "lan", "Lan", "lan@example.com"),
                ProjectRole.MEMBER
        );
        member.setId(17L);
        stubOwner(projectId, ownerId, project);
        when(projectMemberRepository.findByProjectIdAndUserUsername(projectId, "lan"))
                .thenReturn(Optional.of(member));
        when(taskAssignmentRepository.existsByAssigneeIdAndStatus(17L, AssignmentStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> projectMemberService.removeMember(projectId, "lan"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reassign active tasks before removing this project member");

        verify(projectMemberRepository, never()).delete(member);
    }

    @Test
    void updateRoleRejectsViewerDemotionForActiveAssignee() {
        Long ownerId = 7L;
        Long projectId = 42L;
        Project project = project(projectId);
        ProjectMember member = membership(
                project,
                user(9L, "lan", "Lan", "lan@example.com"),
                ProjectRole.MEMBER
        );
        member.setId(17L);
        stubOwner(projectId, ownerId, project);
        when(projectMemberRepository.findByProjectIdAndUserUsername(projectId, "lan"))
                .thenReturn(Optional.of(member));
        when(taskAssignmentRepository.existsByAssigneeIdAndStatus(17L, AssignmentStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> projectMemberService.updateRole(
                projectId,
                "lan",
                new UpdateProjectMemberRoleRequest(ProjectRole.VIEWER)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reassign active tasks before changing this project member to VIEWER");

        verify(projectMemberRepository, never()).save(member);
        assertThat(member.getRole()).isEqualTo(ProjectRole.MEMBER);
    }

    private void stubOwner(Long projectId, Long ownerId, Project project) {
        stubActor(projectId, ownerId, project, ProjectRole.OWNER);
    }

    private void stubActor(Long projectId, Long actorId, Project project, ProjectRole role) {
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(actorId);
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, actorId))
                .thenReturn(Optional.of(membership(
                        project,
                        user(actorId, "actor", "Actor", "actor@example.com"),
                        role
                )));
    }

    private Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        return project;
    }

    private User user(Long id, String username, String displayName, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        return user;
    }

    private ProjectMember membership(Project project, User user, ProjectRole role) {
        ProjectMember membership = new ProjectMember();
        membership.setProject(project);
        membership.setUser(user);
        membership.setRole(role);
        return membership;
    }
}
