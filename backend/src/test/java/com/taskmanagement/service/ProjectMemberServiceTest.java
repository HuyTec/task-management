package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.project.AddProjectMemberRequest;
import com.taskmanagement.dto.project.ProjectMemberResponse;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.DuplicatedResourceException;
import com.taskmanagement.model.Project;
import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.User;
import com.taskmanagement.repository.ProjectMemberRepository;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.service.project.ProjectMemberService;
import com.taskmanagement.utils.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private CustomUserDetails currentUser;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    @Test
    void ownerCanListMembersWithoutExposingIds() {
        Long ownerId = 7L;
        Long projectId = 42L;
        Project project = new Project();
        User memberUser = user(9L, "lan", "Lan", "lan@example.com");
        ProjectMember membership = membership(project, memberUser, ProjectRole.MEMBER);
        membership.setJoinedAt(LocalDateTime.of(2026, 8, 14, 9, 30));
        stubOwner(projectId, ownerId, project);
        when(projectMemberRepository.findByProjectId(projectId)).thenReturn(List.of(membership));

        Response<List<ProjectMemberResponse>> response = projectMemberService.getMembers(projectId);

        assertThat(response.data()).containsExactly(new ProjectMemberResponse(
                "lan", "Lan", "lan@example.com", ProjectRole.MEMBER, membership.getJoinedAt()
        ));
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
                .hasMessage("Project owner cannot be removed");

        verify(projectMemberRepository, never()).delete(ownerMembership);
        verifyNoInteractions(userRepository);
    }

    private void stubOwner(Long projectId, Long ownerId, Project project) {
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(ownerId);
        when(projectRepository.findByIdAndUserId(projectId, ownerId)).thenReturn(Optional.of(project));
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
