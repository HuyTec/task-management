package com.taskmanagement.dto.auth;
import com.taskmanagement.dto.user.UserResponse;

public record AccessInfo(    
    String accessToken, // nội bộ controller và service    
    String refreshToken,
    UserResponse user 
) {
}
