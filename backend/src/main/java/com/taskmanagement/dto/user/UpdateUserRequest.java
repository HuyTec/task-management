package com.taskmanagement.dto.user;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Size(max = 50, min=2, message = "Display name must be at most 50 characters long and at least 2 characters long")
    String displayName,

    @NotBlank(message = "Username is not required")
    String username,

    @NotBlank(message = "Email is not required")
    @Email(message = "Email is invalid")
    String email,

    @NotBlank(message = "Password is not required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    String password
) {
}
