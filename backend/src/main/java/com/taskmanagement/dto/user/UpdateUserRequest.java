package com.taskmanagement.dto.user;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @NotBlank(message = "Display name is not required")
    String displayName,

    @NotBlank(message = "Email is not required")
    @Email(message = "Email is invalid")
    String email,

    @NotBlank(message = "Password is not required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    String password
) {
}
