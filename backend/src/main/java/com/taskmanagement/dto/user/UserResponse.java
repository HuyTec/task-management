package com.taskmanagement.dto.user;
import java.time.LocalDateTime;

import com.taskmanagement.model.UserRole;

public record UserResponse(
    Long id,
    String username,
    String displayName,
    String email,
    UserRole role,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
