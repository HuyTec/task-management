package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.taskmanagement.dto.task.AssignTaskRequest;
import com.taskmanagement.dto.task.RequestChangesRequest;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.DuplicatedResourceException;
import com.taskmanagement.exception.ForbiddenException;
import com.taskmanagement.mapper.TaskWorkflowMapper;
import com.taskmanagement.model.AssignmentStatus;
import com.taskmanagement.model.AssignmentType;
import com.taskmanagement.model.Project;
import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.ReviewDecision;
import com.taskmanagement.model.Task;
import com.taskmanagement.model.TaskAcceptanceCriterion;
import com.taskmanagement.model.TaskAssignment;
import com.taskmanagement.model.TaskReview;
import com.taskmanagement.model.TaskStatus;
import com.taskmanagement.model.User;
import com.taskmanagement.repository.MemberRepository;
import com.taskmanagement.repository.TaskAcceptanceCriterionRepository;
import com.taskmanagement.repository.TaskAssignmentRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.TaskReviewRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.service.task.TaskWorkflowServiceImpl;
import com.taskmanagement.utils.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class TaskWorkflowServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskAssignmentRepository assignmentRepository;
    @Mock private TaskAcceptanceCriterionRepository criterionRepository;
    @Mock private TaskReviewRepository reviewRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private TaskWorkflowMapper workflowMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CustomUserDetails currentUser;

    @InjectMocks
    private TaskWorkflowServiceImpl service;

    @Test
    void memberCanClaimAnUnassignedTodoTask() {
        Task task = task(42L, TaskStatus.TODO);
        ProjectMember member = actor(task, 7L, ProjectRole.MEMBER);
        stubLockedTaskAndActor(task, member);
        when(assignmentRepository.existsByTaskIdAndStatus(42L, AssignmentStatus.ACTIVE))
                .thenReturn(false);
        when(assignmentRepository.saveAndFlush(any(TaskAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.claim(42L);

        ArgumentCaptor<TaskAssignment> assignmentCaptor = ArgumentCaptor.forClass(TaskAssignment.class);
        verify(assignmentRepository).saveAndFlush(assignmentCaptor.capture());
        TaskAssignment assignment = assignmentCaptor.getValue();
        assertThat(assignment.getAssignee()).isSameAs(member);
        assertThat(assignment.getAssignedBy()).isNull();
        assertThat(assignment.getType()).isEqualTo(AssignmentType.CLAIMED);
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.ACTIVE);
        assertThat(assignment.getAssignedAt()).isNotNull();
    }

    @Test
    void claimRejectsASecondActiveAssignment() {
        Task task = task(42L, TaskStatus.TODO);
        ProjectMember member = actor(task, 7L, ProjectRole.MEMBER);
        stubLockedTaskAndActor(task, member);
        when(assignmentRepository.existsByTaskIdAndStatus(42L, AssignmentStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> service.claim(42L))
                .isInstanceOf(DuplicatedResourceException.class)
                .hasMessage("Task already has an active assignment");

        verify(assignmentRepository, never()).saveAndFlush(any(TaskAssignment.class));
    }

    @Test
    void managerCannotAssignTaskToViewer() {
        Task task = task(42L, TaskStatus.TODO);
        ProjectMember manager = actor(task, 7L, ProjectRole.MANAGER);
        ProjectMember viewer = actor(task, 9L, ProjectRole.VIEWER);
        stubLockedTaskAndActor(task, manager);
        when(memberRepository.findByProjectIdAndUserUsername(12L, "viewer"))
                .thenReturn(Optional.of(viewer));

        assertThatThrownBy(() -> service.assign(42L, new AssignTaskRequest("viewer")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("VIEWER cannot be assigned project tasks");

        verify(assignmentRepository, never()).saveAndFlush(any(TaskAssignment.class));
    }

    @Test
    void activeAssigneeCanSubmitTaskForReview() {
        Task task = task(42L, TaskStatus.IN_PROGRESS);
        ProjectMember member = actor(task, 7L, ProjectRole.MEMBER);
        TaskAssignment assignment = assignment(task, member);
        stubLockedTaskAndActor(task, member);
        when(assignmentRepository.findByTaskIdAndStatus(42L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(criterionRepository.findByTaskIdOrderByPositionAsc(42L))
                .thenReturn(List.of(criterion(task, false)));

        service.submitReview(42L);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_REVIEW);
        verify(taskRepository).save(task);
    }

    @Test
    void activeAssigneeCanStartAChangesRequestedTask() {
        Task task = task(42L, TaskStatus.CHANGES_REQUESTED);
        ProjectMember member = actor(task, 7L, ProjectRole.MEMBER);
        TaskAssignment assignment = assignment(task, member);
        stubLockedTaskAndActor(task, member);
        when(assignmentRepository.findByTaskIdAndStatus(42L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));

        service.start(42L);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(taskRepository).save(task);
    }

    @Test
    void requestChangesStoresReasonAndMovesTaskToChangesRequested() {
        Task task = task(42L, TaskStatus.IN_REVIEW);
        ProjectMember manager = actor(task, 7L, ProjectRole.MANAGER);
        stubLockedTaskAndActor(task, manager);
        when(reviewRepository.save(any(TaskReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.requestChanges(42L, new RequestChangesRequest("  Add authorization tests  "));

        ArgumentCaptor<TaskReview> reviewCaptor = ArgumentCaptor.forClass(TaskReview.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().getDecision()).isEqualTo(ReviewDecision.CHANGES_REQUESTED);
        assertThat(reviewCaptor.getValue().getMessage()).isEqualTo("Add authorization tests");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.CHANGES_REQUESTED);
    }

    @Test
    void approveRejectsUnsatisfiedCriteria() {
        Task task = task(42L, TaskStatus.IN_REVIEW);
        ProjectMember owner = actor(task, 7L, ProjectRole.OWNER);
        stubLockedTaskAndActor(task, owner);
        when(criterionRepository.findByTaskIdOrderByPositionAsc(42L))
                .thenReturn(List.of(criterion(task, false)));
        when(criterionRepository.existsByTaskIdAndSatisfiedFalse(42L)).thenReturn(true);

        assertThatThrownBy(() -> service.approve(42L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("All acceptance criteria must be satisfied before approval");

        verify(reviewRepository, never()).save(any(TaskReview.class));
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_REVIEW);
    }

    private void stubLockedTaskAndActor(Task task, ProjectMember actor) {
        when(taskRepository.findByIdForUpdate(task.getId())).thenReturn(Optional.of(task));
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(actor.getUser().getId());
        when(memberRepository.findByProjectIdAndUserId(
                task.getProject().getId(), actor.getUser().getId()
        )).thenReturn(Optional.of(actor));
    }

    private Task task(Long id, TaskStatus status) {
        Project project = new Project();
        project.setId(12L);
        Task task = new Task();
        task.setId(id);
        task.setProject(project);
        task.setStatus(status);
        return task;
    }

    private ProjectMember actor(Task task, Long userId, ProjectRole role) {
        User user = new User();
        user.setId(userId);
        user.setUsername(role.name().toLowerCase());
        ProjectMember member = new ProjectMember();
        member.setId(userId * 10);
        member.setProject(task.getProject());
        member.setUser(user);
        member.setRole(role);
        return member;
    }

    private TaskAssignment assignment(Task task, ProjectMember assignee) {
        TaskAssignment assignment = new TaskAssignment();
        assignment.setTask(task);
        assignment.setAssignee(assignee);
        assignment.setType(AssignmentType.CLAIMED);
        assignment.setStatus(AssignmentStatus.ACTIVE);
        return assignment;
    }

    private TaskAcceptanceCriterion criterion(Task task, boolean satisfied) {
        TaskAcceptanceCriterion criterion = new TaskAcceptanceCriterion();
        criterion.setTask(task);
        criterion.setContent("Acceptance criterion");
        criterion.setPosition(0);
        criterion.setSatisfied(satisfied);
        return criterion;
    }
}
