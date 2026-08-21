package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.taskmanagement.dto.submission.CreateLinkEvidenceRequest;
import com.taskmanagement.dto.submission.InitiateFileEvidenceRequest;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.ForbiddenException;
import com.taskmanagement.model.AssignmentStatus;
import com.taskmanagement.model.AssignmentType;
import com.taskmanagement.model.EvidenceProvider;
import com.taskmanagement.model.EvidenceType;
import com.taskmanagement.model.Project;
import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.Submission;
import com.taskmanagement.model.SubmissionStatus;
import com.taskmanagement.model.Task;
import com.taskmanagement.model.TaskAssignment;
import com.taskmanagement.model.TaskEvidence;
import com.taskmanagement.model.TaskStatus;
import com.taskmanagement.model.User;
import com.taskmanagement.repository.MemberRepository;
import com.taskmanagement.repository.SubmissionRepository;
import com.taskmanagement.repository.TaskAcceptanceCriterionRepository;
import com.taskmanagement.repository.TaskAssignmentRepository;
import com.taskmanagement.repository.TaskEvidenceRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.service.submission.EvidenceStorage;
import com.taskmanagement.service.submission.SubmissionService;
import com.taskmanagement.utils.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {
    @Mock private SubmissionRepository submissionRepository;
    @Mock private TaskEvidenceRepository evidenceRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private TaskAssignmentRepository assignmentRepository;
    @Mock private TaskAcceptanceCriterionRepository criterionRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private EvidenceStorage evidenceStorage;
    @Mock private CustomUserDetails currentUser;

    @InjectMocks private SubmissionService service;

    private Task task;
    private ProjectMember actor;

    @BeforeEach
    void setUp() {
        Project project = new Project();
        project.setId(12L);
        task = new Task();
        task.setId(42L);
        task.setProject(project);
        task.setStatus(TaskStatus.IN_PROGRESS);
        actor = member(project, 7L, ProjectRole.MEMBER);
    }

    @Test
    void assigneeOfAnotherTaskCannotCreateSubmission() {
        ProjectMember otherAssignee = member(task.getProject(), 9L, ProjectRole.MEMBER);
        stubActorForLockedTask();
        when(assignmentRepository.findByTaskIdAndStatus(42L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment(otherAssignee)));

        assertThatThrownBy(() -> service.create(42L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only the active assignee can create or submit work");

        verify(submissionRepository, never()).saveAndFlush(any(Submission.class));
    }

    @Test
    void fileLargerThanTwentyFiveMiBIsRejectedBeforeStorageCall() {
        Submission submission = draftSubmission();
        stubActorForSubmission(submission);

        assertThatThrownBy(() -> service.initiateFile(
                5L,
                new InitiateFileEvidenceRequest(
                        "large.pdf", "application/pdf", SubmissionService.MAX_FILE_SIZE + 1
                )
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("File size must not exceed 25 MiB");

        verify(evidenceStorage, never()).createUpload(any(), any(), anyLong());
        verify(evidenceRepository, never()).saveAndFlush(any(TaskEvidence.class));
    }

    @Test
    void eleventhEvidenceIsRejectedAsBadRequest() {
        Submission submission = draftSubmission();
        stubActorForSubmission(submission);
        when(evidenceRepository.countBySubmissionId(5L)).thenReturn(10L);

        assertThatThrownBy(() -> service.addLink(
                5L,
                new CreateLinkEvidenceRequest(
                        EvidenceType.EXTERNAL_LINK,
                        EvidenceProvider.OTHER,
                        "Result",
                        "https://example.com/result"
                )
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A submission can contain at most 10 evidence items");

        verify(evidenceRepository, never()).saveAndFlush(any(TaskEvidence.class));
    }

    private void stubActorForLockedTask() {
        when(taskRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(task));
        stubCurrentMember();
    }

    private void stubActorForSubmission(Submission submission) {
        when(submissionRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(submission));
        stubCurrentMember();
    }

    private void stubCurrentMember() {
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(7L);
        when(memberRepository.findByProjectIdAndUserId(12L, 7L)).thenReturn(Optional.of(actor));
    }

    private Submission draftSubmission() {
        Submission submission = new Submission();
        submission.setId(5L);
        submission.setTask(task);
        submission.setAssignee(actor);
        submission.setStatus(SubmissionStatus.DRAFT);
        return submission;
    }

    private ProjectMember member(Project project, Long userId, ProjectRole role) {
        User user = new User();
        user.setId(userId);
        user.setUsername("user" + userId);
        ProjectMember member = new ProjectMember();
        member.setId(userId * 10);
        member.setProject(project);
        member.setUser(user);
        member.setRole(role);
        return member;
    }

    private TaskAssignment assignment(ProjectMember assignee) {
        return TaskAssignment.builder()
                .task(task)
                .assignee(assignee)
                .type(AssignmentType.ASSIGNED)
                .status(AssignmentStatus.ACTIVE)
                .build();
    }
}
