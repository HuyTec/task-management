package com.taskmanagement.dto.auth;
import com.taskmanagement.dto.user.UserResponse;

public record AccessInfo(    
    String accessToken,
    String refreshToken,
    UserResponse user 
) {
}
