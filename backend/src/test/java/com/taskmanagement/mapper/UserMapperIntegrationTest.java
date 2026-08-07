package com.taskmanagement.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.taskmanagement.dto.user.CreateUserRequest;
import com.taskmanagement.model.User;

class UserMapperIntegrationTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void mapsCreateRequestWithoutManagedFields() {
        CreateUserRequest request = new CreateUserRequest(
                "alice", "Alice", "alice@example.com", "password123");

        User user = userMapper.toUser(request);

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getDisplayName()).isEqualTo("Alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getId()).isNull();
        assertThat(user.getRole()).isNull();
    }
}
