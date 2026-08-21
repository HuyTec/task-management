package com.taskmanagement.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
        @NotBlank(message = "Google credential cannot be blank")
        @Size(max = 8192, message = "Google credential is too large")
        String credential
) {
}
