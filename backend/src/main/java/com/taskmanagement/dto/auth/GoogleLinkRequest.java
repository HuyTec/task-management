package com.taskmanagement.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLinkRequest(
        @NotBlank(message = "Google credential cannot be blank")
        @Size(max = 8192, message = "Google credential is too large")
        String credential,

        @NotBlank(message = "Current password is required")
        @Size(max = 255, message = "Password is too large")
        String password
) {
}
