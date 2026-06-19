package com.taskmanagement.dto.auth;

import jakarta.validation.constraints.NotBlank;
import com.taskmanagement.dto.user.UserResponse;

public record AuthResponse(
    @NotBlank(message = "Access token is required")
    String accessToken,

    @NotBlank(message = "Refresh token is required")
    String refreshToken,

    UserResponse user
) {
}
