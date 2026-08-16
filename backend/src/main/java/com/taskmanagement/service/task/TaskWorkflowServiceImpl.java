package com.taskmanagement.service.task;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.task.AcceptanceCriterionResponse;
import com.taskmanagement.dto.task.AssignTaskRequest;
import com.taskmanagement.dto.task.CreateAcceptanceCriterionRequest;
import com.taskmanagement.dto.task.RequestChangesRequest;
import com.taskmanagement.dto.task.TaskAssignmentResponse;
import com.taskmanagement.dto.task.TaskReviewResponse;
import com.taskmanagement.dto.task.TaskWorkflowResponse;
import com.taskmanagement.dto.task.UpdateAcceptanceCriterionRequest;
import com.taskmanagement.event.TaskCacheEvictEvent;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.exception.DuplicatedResourceException;
import com.taskmanagement.exception.ForbiddenException;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.mapper.TaskWorkflowMapper;
import com.taskmanagement.model.AssignmentStatus;
import com.taskmanagement.model.AssignmentType;
import com.taskmanagement.model.ProjectMember;
import com.taskmanagement.model.ProjectRole;
import com.taskmanagement.model.ReviewDecision;
import com.taskmanagement.model.Task;
import com.taskmanagement.model.TaskAcceptanceCriterion;
import com.taskmanagement.model.TaskAssignment;
import com.taskmanagement.model.TaskReview;
import com.taskmanagement.model.TaskStatus;
import com.taskmanagement.repository.MemberRepository;
import com.taskmanagement.repository.TaskAcceptanceCriterionRepository;
import com.taskmanagement.repository.TaskAssignmentRepository;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.TaskReviewRepository;
import com.taskmanagement.security.CustomUserDetails;
import com.taskmanagement.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskWorkflowServiceImpl implements TaskWorkflowService {

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final TaskAcceptanceCriterionRepository criterionRepository;
    private final TaskReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final TaskWorkflowMapper workflowMapper;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Response<TaskAssignmentResponse> claim(Long taskId) {
        Task task = requireLockedProjectTask(taskId);
        ProjectMember actor = requireCurrentMember(task);
        if (actor.getRole() != ProjectRole.MEMBER) {
            throw new ForbiddenException("Only project MEMBER can claim an unassigned task");
        }
        requireTaskOpenForAssignment(task);
        requireNoActiveAssignment(taskId);

        TaskAssignment assignment = newAssignment(
                task,
                actor,
                null,
                AssignmentType.CLAIMED
        );
        TaskAssignment saved = assignmentRepository.saveAndFlush(assignment);
        evictTaskForProjectMembers(task);
        return Response.success(workflowMapper.toAssignmentResponse(saved), "Task claimed successfully!");
    }

    @Override
    public Response<Void> releaseClaim(Long taskId) {
        Task task = requireLockedProjectTask(taskId);
        ProjectMember actor = requireCurrentMember(task);
        TaskAssignment assignment = requireActiveAssignment(taskId);
        if (assignment.getType() != AssignmentType.CLAIMED
                || !assignment.getAssignee().getId().equals(actor.getId())) {
            throw new ForbiddenException("Only the member who claimed this task can release the claim");
        }

        // TODO BUSINESS RULE: decide whether a claim may be released after work starts.
        // Safe default: require manager reassignment once the task has left TODO.
        requireTaskOpenForAssignment(task);
        cancelAssignment(assignment);
        evictTaskForProjectMembers(task);
        return Response.success(null, "Task claim released successfully!");
    }

    @Override
    public Response<TaskAssignmentResponse> assign(Long taskId, AssignTaskRequest request) {
        Task task = requireLockedProjectTask(taskId);
        ProjectMember manager = requireCurrentManager(task);
        requireAssignmentMutable(task);

        ProjectMember assignee = memberRepository.findByProjectIdAndUserUsername(
                        task.getProject().getId(),
                        request.username().trim()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Project member not found!"));
        if (assignee.getRole() == ProjectRole.VIEWER) {
            throw new ForbiddenException("VIEWER cannot be assigned project tasks");
        }

        // TODO BUSINESS RULE: decide whether OWNER/MANAGER may also be assignees.
        // Current contract permits every active project role except VIEWER.
        assignmentRepository.findByTaskIdAndStatus(taskId, AssignmentStatus.ACTIVE)
                .ifPresent(this::cancelAssignment);
        assignmentRepository.flush();

        TaskAssignment saved = assignmentRepository.saveAndFlush(newAssignment(
                task,
                assignee,
                manager,
                AssignmentType.ASSIGNED
        ));
        evictTaskForProjectMembers(task);
        return Response.success(workflowMapper.toAssignmentResponse(saved), "Task assigned successfully!");
    }

    @Override
    public Response<Void> clearAssignee(Long taskId) {
        Task task = requireLockedProjectTask(taskId);
        requireCurrentManager(task);
        requireAssignmentMutable(task);
        cancelAssignment(requireActiveAssignment(taskId));
        evictTaskForProjectMembers(task);
        return Response.success(null, "Task assignee removed successfully!");
    }

    @Override
    public Response<TaskWorkflowResponse> start(Long taskId) {
        Task task = requireLockedProjectTask(taskId);
        ProjectMember actor = requireCurrentMember(task);
        requireCurrentAssignee(taskId, actor);
        if (task.getStatus() != TaskStatus.TODO
                && task.getStatus() != TaskStatus.CHANGES_REQUESTED) {
            throw new BadRequestException(
                    "Only TODO or CHANGES_REQUESTED task can be started"
            );
        }

        task.setStatus(TaskStatus.IN_PROGRESS);
        taskRepository.save(task);
        evictTaskForProjectMembers(task);
        return Response.success(
                new TaskWorkflowResponse(task.getId(), task.getStatus()),
                "Task started successfully!"
        );
    }

    @Override
    public Response<AcceptanceCriterionResponse> addCriterion(
            Long taskId,
            CreateAcceptanceCriterionRequest request
    ) {
        Task task = requireLockedProjectTask(taskId);
        requireCurrentManager(task);
        requireCriteriaStructureMutable(task);

        TaskAcceptanceCriterion criterion = new TaskAcceptanceCriterion();
        criterion.setTask(task);
        criterion.setContent(request.content().trim());
        criterion.setPosition(request.position());
        criterion.setSatisfied(false);
        TaskAcceptanceCriterion saved = criterionRepository.save(criterion);
        evictTaskForProjectMembers(task);
        return Response.success(workflowMapper.toCriterionResponse(saved), "Acceptance criterion added successfully!");
    }

    @Override
    public Response<AcceptanceCriterionResponse> updateCriterion(
            Long taskId,
            Long criterionId,
            UpdateAcceptanceCriterionRequest request
    ) {
        Task task = requireLockedProjectTask(taskId);
        requireCurrentManager(task);
        TaskAcceptanceCriterion criterion = criterionRepository.findByIdAndTaskId(criterionId, taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Acceptance criterion not found!"));

        if (request.content() == null && request.satisfied() == null && request.position() == null) {
            throw new BadRequestException("At least one criterion field must be provided");
        }
        if (request.content() != null || request.position() != null) {
            // TODO BUSINESS RULE: decide whether criteria scope may change after work starts.
            // Safe default freezes content/order once the task leaves TODO.
            requireCriteriaStructureMutable(task);
        }
        if (request.satisfied() != null && task.getStatus() != TaskStatus.IN_REVIEW) {
            throw new BadRequestException("Criteria can only be reviewed while task is IN_REVIEW");
        }
        if (request.content() != null) {
            if (request.content().isBlank()) {
                throw new BadRequestException("Criterion content cannot be blank");
            }
            criterion.setContent(request.content().trim());
        }
        if (request.position() != null) {
            criterion.setPosition(request.position());
        }
        if (request.satisfied() != null) {
            criterion.setSatisfied(request.satisfied());
        }

        TaskAcceptanceCriterion saved = criterionRepository.save(criterion);
        evictTaskForProjectMembers(task);
        return Response.success(workflowMapper.toCriterionResponse(saved), "Acceptance criterion updated successfully!");
    }

    @Override
    public Response<Void> deleteCriterion(Long taskId, Long criterionId) {
        Task task = requireLockedProjectTask(taskId);
        requireCurrentManager(task);
        requireCriteriaStructureMutable(task);
        TaskAcceptanceCriterion criterion = criterionRepository.findByIdAndTaskId(criterionId, taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Acceptance criterion not found!"));
        criterionRepository.delete(criterion);
        evictTaskForProjectMembers(task);
        return Response.success(null, "Acceptance criterion deleted successfully!");
    }

    @Override
    public Response<TaskWorkflowResponse> submitReview(Long taskId) {
        Task task = requireLockedProjectTask(taskId);
        ProjectMember actor = requireCurrentMember(task);
        requireCurrentAssignee(taskId, actor);
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new BadRequestException("Only IN_PROGRESS task can be submitted for review");
        }
        if (criterionRepository.findByTaskIdOrderByPositionAsc(taskId).isEmpty()) {
            throw new BadRequestException("Task requires at least one acceptance criterion before review");
        }

        task.setStatus(TaskStatus.IN_REVIEW);
        taskRepository.save(task);
        evictTaskForProjectMembers(task);
        return Response.success(
                new TaskWorkflowResponse(task.getId(), task.getStatus()),
                "Task submitted for review successfully!"
        );
    }

    @Override
    public Response<TaskReviewResponse> requestChanges(
            Long taskId,
            RequestChangesRequest request
    ) {
        Task task = requireLockedProjectTask(taskId);
        ProjectMember reviewer = requireCurrentManager(task);
        requireInReview(task);

        TaskReview review = newReview(
                task,
                reviewer,
                ReviewDecision.CHANGES_REQUESTED,
                request.message().trim()
        );
        TaskReview saved = reviewRepository.save(review);
        task.setStatus(TaskStatus.CHANGES_REQUESTED);
        taskRepository.save(task);
        evictTaskForProjectMembers(task);
        return Response.success(workflowMapper.toReviewResponse(saved), "Task changes requested successfully!");
    }

    @Override
    public Response<TaskReviewResponse> approve(Long taskId) {
        Task task = requireLockedProjectTask(taskId);
        ProjectMember reviewer = requireCurrentManager(task);
        requireInReview(task);
        List<TaskAcceptanceCriterion> criteria = criterionRepository.findByTaskIdOrderByPositionAsc(taskId);
        if (criteria.isEmpty()) {
            throw new BadRequestException("Task requires at least one acceptance criterion before approval");
        }
        if (criterionRepository.existsByTaskIdAndSatisfiedFalse(taskId)) {
            throw new BadRequestException("All acceptance criteria must be satisfied before approval");
        }

        // TODO BUSINESS RULE: decide whether a manager may review a task assigned to themselves.
        TaskReview review = newReview(task, reviewer, ReviewDecision.APPROVED, null);
        TaskReview saved = reviewRepository.save(review);
        task.setStatus(TaskStatus.DONE);
        taskRepository.save(task);
        evictTaskForProjectMembers(task);
        return Response.success(workflowMapper.toReviewResponse(saved), "Task approved successfully!");
    }

    private Task requireLockedProjectTask(Long taskId) {
        Task task = taskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found!"));
        if (task.getProject() == null) {
            throw new BadRequestException("Assignment, criteria and review require a project task");
        }
        return task;
    }

    private ProjectMember requireCurrentMember(Task task) {
        CustomUserDetails currentUser = securityUtils.getCurrentUser();
        return memberRepository.findByProjectIdAndUserId(task.getProject().getId(), currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found!"));
    }

    private ProjectMember requireCurrentManager(Task task) {
        ProjectMember actor = requireCurrentMember(task);
        if (actor.getRole() != ProjectRole.OWNER && actor.getRole() != ProjectRole.MANAGER) {
            throw new ForbiddenException("Task management requires OWNER or MANAGER role");
        }
        return actor;
    }

    private void requireTaskOpenForAssignment(Task task) {
        if (task.getStatus() != TaskStatus.TODO) {
            throw new BadRequestException("Only TODO task can be claimed or released");
        }
    }

    private void requireAssignmentMutable(Task task) {
        if (task.getStatus() == TaskStatus.IN_REVIEW || task.getStatus() == TaskStatus.DONE) {
            throw new BadRequestException("Assignee cannot change while task is IN_REVIEW or DONE");
        }
    }

    private void requireCriteriaStructureMutable(Task task) {
        if (task.getStatus() != TaskStatus.TODO) {
            throw new BadRequestException("Criteria content and order can only change while task is TODO");
        }
    }

    private void requireNoActiveAssignment(Long taskId) {
        if (assignmentRepository.existsByTaskIdAndStatus(taskId, AssignmentStatus.ACTIVE)) {
            throw new DuplicatedResourceException("Task already has an active assignment");
        }
    }

    private TaskAssignment requireActiveAssignment(Long taskId) {
        return assignmentRepository.findByTaskIdAndStatus(taskId, AssignmentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active task assignment not found!"));
    }

    private void requireCurrentAssignee(Long taskId, ProjectMember actor) {
        TaskAssignment assignment = requireActiveAssignment(taskId);
        if (!assignment.getAssignee().getId().equals(actor.getId())) {
            throw new ForbiddenException("Only the active assignee can submit this task for review");
        }
    }

    private void requireInReview(Task task) {
        if (task.getStatus() != TaskStatus.IN_REVIEW) {
            throw new BadRequestException("Task must be IN_REVIEW");
        }
    }

    private TaskAssignment newAssignment(
            Task task,
            ProjectMember assignee,
            ProjectMember assignedBy,
            AssignmentType type
    ) {
        return TaskAssignment.builder()
                .task(task)
                .assignee(assignee)
                .assignedBy(assignedBy)
                .type(type)
                .status(AssignmentStatus.ACTIVE)
                .assignedAt(LocalDateTime.now())
                .build();
    }

    private void cancelAssignment(TaskAssignment assignment) {
        assignment.setStatus(AssignmentStatus.CANCELLED);
        assignment.setEndedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
    }

    private TaskReview newReview(
            Task task,
            ProjectMember reviewer,
            ReviewDecision decision,
            String message
    ) {
        TaskReview review = new TaskReview();
        review.setTask(task);
        review.setReviewer(reviewer);
        review.setDecision(decision);
        review.setMessage(message);
        return review;
    }

    private void evictTaskForProjectMembers(Task task) {
        memberRepository.findByProjectId(task.getProject().getId()).forEach(member ->
                eventPublisher.publishEvent(new TaskCacheEvictEvent(member.getUser().getId(), task.getId()))
        );
    }

}
