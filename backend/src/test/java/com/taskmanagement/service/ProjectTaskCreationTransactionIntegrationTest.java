package com.taskmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.taskmanagement.dto.task.CreateProjectTaskRequest;
import com.taskmanagement.mapper.TaskMapperImpl;
import com.taskmanagement.mapper.TaskWorkflowMapperImpl;
import com.taskmanagement.model.Project;
import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.ProjectStatus;
import com.taskmanagement.model.TaskPriority;
import com.taskmanagement.model.User;
import com.taskmanagement.model.UserRole;
import com.taskmanagement.repository.MemberRepository;
import com.taskmanagement.repository.ProjectRepository;
import com.taskmanagement.repository.TaskAcceptanceCriterionRepository;
import com.taskmanagement.repository.TaskAssignmentRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.service.task.TaskWorkflowServiceImpl;
import com.taskmanagement.utils.SecurityUtils;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({TaskWorkflowServiceImpl.class, TaskMapperImpl.class, TaskWorkflowMapperImpl.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProjectTaskCreationTransactionIntegrationTest {

    @Autowired private TaskWorkflowServiceImpl service;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private TaskAssignmentRepository assignmentRepository;
    @MockitoSpyBean private TaskAcceptanceCriterionRepository criterionRepository;
    @MockitoBean private SecurityUtils securityUtils;
    @MockitoBean private ApplicationEventPublisher eventPublisher;

    private final CustomUserDetails currentUser = org.mockito.Mockito.mock(CustomUserDetails.class);
    private Long projectId;
    private Long ownerId;

    @BeforeEach
    void setUpProject() {
        transactionTemplate.executeWithoutResult(status -> {
            User owner = new User();
            owner.setUsername("transaction-owner");
            owner.setDisplayName("Transaction Owner");
            owner.setEmail("transaction-owner@example.com");
            owner.setPassword("encoded-password");
            owner.setRole(UserRole.USER);
            owner = userRepository.saveAndFlush(owner);

            Project project = new Project();
            project.setName("Atomic project");
            project.setDescription("Rollback integration fixture");
            project.setStartDate(LocalDate.of(2026, 8, 1));
            project.setEndDate(LocalDate.of(2026, 8, 31));
            project.setStatus(ProjectStatus.ACTIVE);
            project.setUser(owner);
            project = projectRepository.saveAndFlush(project);

            ProjectMember membership = new ProjectMember();
            membership.setProject(project);
            membership.setUser(owner);
            membership.setRole(ProjectRole.OWNER);
            memberRepository.saveAndFlush(membership);

            ownerId = owner.getId();
            projectId = project.getId();
        });
        when(securityUtils.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getId()).thenReturn(ownerId);
    }

    @Test
    void criterionPersistenceFailureRollsBackTheNewTask() {
        doThrow(new DataIntegrityViolationException("simulated criterion failure"))
                .when(criterionRepository).saveAll(any());
        CreateProjectTaskRequest request = new CreateProjectTaskRequest(
                "Atomic task",
                "No partial task may remain",
                TaskPriority.HIGH,
                LocalDate.of(2026, 8, 25),
                List.of("Observable result"),
                null
        );

        assertThatThrownBy(() -> service.createProjectTask(projectId, request))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("simulated criterion failure");

        assertThat(taskRepository.count()).isZero();
        assertThat(criterionRepository.count()).isZero();
        assertThat(assignmentRepository.count()).isZero();
    }
}
