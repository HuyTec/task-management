package com.taskmanagement.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.taskmanagement.dto.task.TaskFilter;
import com.taskmanagement.dto.task.TaskWorkspaceView;
import com.taskmanagement.model.AssignmentStatus;
import com.taskmanagement.model.AssignmentType;
import com.taskmanagement.model.Project;
import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.Task;
import com.taskmanagement.model.TaskAssignment;
import com.taskmanagement.model.TaskPriority;
import com.taskmanagement.model.TaskStatus;
import com.taskmanagement.model.User;
import com.taskmanagement.model.UserRole;
import com.taskmanagement.repository.specification.TaskSpecifications;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TaskSpecificationsIntegrationTest {

    private static final TaskFilter NO_FILTER = new TaskFilter(
            null, null, null, null, null, null
    );

    @Autowired private EntityManager entityManager;
    @Autowired private TaskRepository taskRepository;

    private User manager;
    private User member;
    private ProjectMember memberMembership;
    private Task personalTask;
    private Task assignedTask;
    private Task accessibleTask;
    private Task reviewTask;

    @BeforeEach
    void setUpWorkspace() {
        User owner = persistUser("owner");
        manager = persistUser("manager");
        member = persistUser("member");
        Project project = persistProject(owner);
        persistMembership(project, owner, ProjectRole.OWNER);
        persistMembership(project, manager, ProjectRole.MANAGER);
        memberMembership = persistMembership(project, member, ProjectRole.MEMBER);

        personalTask = persistTask("Personal study", member, null, TaskStatus.TODO);
        assignedTask = persistTask("Assigned chapter", owner, project, TaskStatus.TODO);
        accessibleTask = persistTask("Shared notes", owner, project, TaskStatus.TODO);
        reviewTask = persistTask("Review exercise", owner, project, TaskStatus.IN_REVIEW);
        persistAssignment(assignedTask, memberMembership);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void myWorkContainsPersonalAndActivelyAssignedTasksOnly() {
        List<Long> taskIds = taskRepository.findAll(TaskSpecifications.forWorkspace(
                        member.getId(), TaskWorkspaceView.MY_WORK, NO_FILTER
                )).stream()
                .map(Task::getId)
                .toList();

        assertThat(taskIds)
                .containsExactlyInAnyOrder(personalTask.getId(), assignedTask.getId())
                .doesNotContain(accessibleTask.getId(), reviewTask.getId());
    }

    @Test
    void allAccessibleContainsEveryProjectTaskForAMember() {
        List<Long> taskIds = taskRepository.findAll(TaskSpecifications.forWorkspace(
                        member.getId(), TaskWorkspaceView.ALL_ACCESSIBLE, NO_FILTER
                )).stream()
                .map(Task::getId)
                .toList();

        assertThat(taskIds).containsExactlyInAnyOrder(
                personalTask.getId(),
                assignedTask.getId(),
                accessibleTask.getId(),
                reviewTask.getId()
        );
    }

    @Test
    void reviewQueueRequiresManagerRoleAndInReviewStatus() {
        List<Long> managerTaskIds = taskRepository.findAll(TaskSpecifications.forWorkspace(
                        manager.getId(), TaskWorkspaceView.REVIEW_QUEUE, NO_FILTER
                )).stream()
                .map(Task::getId)
                .toList();
        List<Task> memberTasks = taskRepository.findAll(TaskSpecifications.forWorkspace(
                member.getId(), TaskWorkspaceView.REVIEW_QUEUE, NO_FILTER
        ));

        assertThat(managerTaskIds).containsExactly(reviewTask.getId());
        assertThat(memberTasks).isEmpty();
    }

    private User persistUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setDisplayName(username);
        user.setEmail(username + "@example.com");
        user.setPassword("encoded-password");
        user.setRole(UserRole.USER);
        entityManager.persist(user);
        return user;
    }

    private Project persistProject(User owner) {
        Project project = new Project();
        project.setName("Learning project");
        project.setDescription("Specification integration fixture");
        project.setStartDate(LocalDate.now());
        project.setEndDate(LocalDate.now().plusMonths(1));
        project.setUser(owner);
        entityManager.persist(project);
        return project;
    }

    private ProjectMember persistMembership(Project project, User user, ProjectRole role) {
        ProjectMember membership = new ProjectMember();
        membership.setProject(project);
        membership.setUser(user);
        membership.setRole(role);
        entityManager.persist(membership);
        return membership;
    }

    private Task persistTask(String title, User creator, Project project, TaskStatus status) {
        Task task = new Task();
        task.setTitle(title);
        task.setUser(creator);
        task.setProject(project);
        task.setStatus(status);
        task.setPriority(TaskPriority.MEDIUM);
        entityManager.persist(task);
        return task;
    }

    private void persistAssignment(Task task, ProjectMember assignee) {
        TaskAssignment assignment = TaskAssignment.builder()
                .task(task)
                .assignee(assignee)
                .type(AssignmentType.CLAIMED)
                .status(AssignmentStatus.ACTIVE)
                .assignedAt(LocalDateTime.now())
                .build();
        entityManager.persist(assignment);
    }
}
