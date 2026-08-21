package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.taskmanagement.exception.ConflictException;
import com.taskmanagement.model.AssignmentStatus;
import com.taskmanagement.model.AssignmentType;
import com.taskmanagement.model.EvidenceProvider;
import com.taskmanagement.model.EvidenceType;
import com.taskmanagement.model.Project;
import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.ProjectStatus;
import com.taskmanagement.model.Submission;
import com.taskmanagement.model.SubmissionStatus;
import com.taskmanagement.model.Task;
import com.taskmanagement.model.TaskAssignment;
import com.taskmanagement.model.TaskEvidence;
import com.taskmanagement.model.TaskPriority;
import com.taskmanagement.model.TaskStatus;
import com.taskmanagement.model.User;
import com.taskmanagement.model.UserRole;
import com.taskmanagement.repository.MemberRepository;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.SubmissionRepository;
import com.taskmanagement.repository.TaskAssignmentRepository;
import com.taskmanagement.repository.TaskEvidenceRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.service.submission.EvidenceStorage;
import com.taskmanagement.service.submission.SubmissionService;
import com.taskmanagement.utils.SecurityUtils;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(SubmissionService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SubmissionImmutabilityIntegrationTest {
    @Autowired private SubmissionService service;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private TaskAssignmentRepository assignmentRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private TaskEvidenceRepository evidenceRepository;
    @MockitoBean private SecurityUtils securityUtils;
    @MockitoBean private EvidenceStorage evidenceStorage;

    private final CustomUserDetails currentUser = org.mockito.Mockito.mock(CustomUserDetails.class);
    private Long userId;
    private Long submissionId;
    private Long evidenceId;

    @BeforeEach
    void setUpSubmittedEvidence() {
        transactionTemplate.executeWithoutResult(status -> {
            User user = new User();
            user.setUsername("submission-owner");
            user.setDisplayName("Submission Owner");
            user.setEmail("submission-owner@example.com");
            user.setPassword("encoded-password");
            user.setRole(UserRole.USER);
            user = userRepository.saveAndFlush(user);

            Project project = new Project();
            project.setName("Evidence project");
            project.setDescription("Immutability fixture");
            project.setStartDate(LocalDate.of(2026, 8, 1));
            project.setEndDate(LocalDate.of(2026, 8, 31));
            project.setStatus(ProjectStatus.ACTIVE);
            project.setUser(user);
            project = projectRepository.saveAndFlush(project);

            ProjectMember membership = new ProjectMember();
            membership.setProject(project);
            membership.setUser(user);
            membership.setRole(ProjectRole.MEMBER);
            membership = memberRepository.saveAndFlush(membership);

            Task task = new Task();
            task.setTitle("Submitted task");
            task.setDescription("Evidence is immutable");
            task.setPriority(TaskPriority.HIGH);
            task.setStatus(TaskStatus.IN_REVIEW);
            task.setUser(user);
            task.setProject(project);
            task = taskRepository.saveAndFlush(task);

            TaskAssignment assignment = TaskAssignment.builder()
                    .task(task).assignee(membership).type(AssignmentType.ASSIGNED)
                    .status(AssignmentStatus.ACTIVE).assignedAt(LocalDateTime.now()).build();
            assignmentRepository.saveAndFlush(assignment);

            Submission submission = new Submission();
            submission.setTask(task);
            submission.setAssignee(membership);
            submission.setSequenceNumber(1);
            submission.setStatus(SubmissionStatus.SUBMITTED);
            submission.setSubmittedAt(LocalDateTime.now());
            submission = submissionRepository.saveAndFlush(submission);

            TaskEvidence evidence = new TaskEvidence();
            evidence.setSubmission(submission);
            evidence.setEvidenceType(EvidenceType.EXTERNAL_LINK);
            evidence.setProvider(EvidenceProvider.OTHER);
            evidence.setDisplayName("Result");
            evidence.setUrl("https://example.com/result");
            evidence = evidenceRepository.saveAndFlush(evidence);

            userId = user.getId();
            submissionId = submission.getId();
            evidenceId = evidence.getId();
        });
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(userId);
    }

    @Test
    void submittedEvidenceCannotBeDeletedAndRemainsPersisted() {
        assertThatThrownBy(() -> service.deleteEvidence(submissionId, evidenceId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Submitted evidence is immutable");

        assertThat(evidenceRepository.findById(evidenceId)).isPresent();
    }
}
