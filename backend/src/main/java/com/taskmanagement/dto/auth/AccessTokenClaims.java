package com.taskmanagement.dto.auth;

public record AccessTokenClaims(
    String username, 
    String sessionId
) {}


