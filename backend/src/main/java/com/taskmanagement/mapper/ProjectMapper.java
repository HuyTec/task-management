package com.taskmanagement.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.taskmanagement.dto.project.CreateProjectRequest;
import com.taskmanagement.dto.project.ProjectResponse;
import com.taskmanagement.model.Project;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "currentUserRole", ignore = true)
    ProjectResponse toProjectResponse(Project project);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Project toProject(CreateProjectRequest request);
}
