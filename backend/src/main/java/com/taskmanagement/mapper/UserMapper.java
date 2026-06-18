package com.taskmanagement.mapper;
import com.taskmanagement.dto.user.CreateUserRequest;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    public UserResponse toUserResponse(User user);

    public User toUser(CreateUserRequest request);
}
