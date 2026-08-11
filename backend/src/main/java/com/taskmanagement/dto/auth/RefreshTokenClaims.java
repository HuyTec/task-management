package com.taskmanagement.dto.auth;

public record RefreshTokenClaims(
        String username,
        String sessionId,
        String tokenId
) {}