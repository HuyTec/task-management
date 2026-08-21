package com.taskmanagement.dto.auth;

/**
 * Identity claims accepted only after the Google ID token has been verified.
 * The subject is the stable provider identifier; email is profile data and must
 * not be used as the federated identity key.
 */
public record GoogleProfile(
        String subject,
        String email,
        String displayName,
        String profilePictureUrl
) {
}
