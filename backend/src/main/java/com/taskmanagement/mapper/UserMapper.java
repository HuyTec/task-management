package com.taskmanagement.mapper;
import com.taskmanagement.dto.user.CreateUserRequest;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.model.User;
import io.micrometer.common.lang.NonNull;
import org.mapstruct.Mapping;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    public UserResponse toUserResponse(@NonNull User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "role", ignore = true)
    public User toUser(@NonNull CreateUserRequest request);
}
