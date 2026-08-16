package com.taskmanagement.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.taskmanagement.dto.task.AcceptanceCriterionResponse;
import com.taskmanagement.dto.task.TaskAssignmentResponse;
import com.taskmanagement.dto.task.TaskReviewResponse;
import com.taskmanagement.model.TaskAcceptanceCriterion;
import com.taskmanagement.model.TaskAssignment;
import com.taskmanagement.model.TaskReview;

@Mapper(componentModel = "spring")
public interface TaskWorkflowMapper {

    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "assigneeUsername", source = "assignee.user.username")
    @Mapping(target = "assigneeDisplayName", source = "assignee.user.displayName")
    @Mapping(target = "assignedByUsername", source = "assignedBy.user.username")
    TaskAssignmentResponse toAssignmentResponse(TaskAssignment assignment);

    AcceptanceCriterionResponse toCriterionResponse(TaskAcceptanceCriterion criterion);

    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "reviewerUsername", source = "reviewer.user.username")
    @Mapping(target = "taskStatus", source = "task.status")
    TaskReviewResponse toReviewResponse(TaskReview review);
}
