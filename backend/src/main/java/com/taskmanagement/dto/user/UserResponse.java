    package com.taskmanagement.dto.user;
    import java.time.LocalDateTime;

    import com.taskmanagement.model.UserRole;

    public record UserResponse(
        Long id,
        String username,
        String displayName,
        String email,
        String profilePictureUrl,
        UserRole role,
        boolean deactivated,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }
