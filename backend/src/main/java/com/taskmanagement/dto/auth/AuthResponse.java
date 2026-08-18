package com.taskmanagement.dto.auth;

import com.taskmanagement.dto.user.UserResponse;

public record AuthResponse(
    String accessToken,
    UserResponse user
) {
}
