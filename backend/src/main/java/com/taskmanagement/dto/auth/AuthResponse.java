package com.taskmanagement.dto.auth;

import com.taskmanagement.dto.user.UserResponse;

public record AuthResponse( // Tới Frontend
    String accessToken, // Nếu Jwt hoạt động tốt thì ở đây không bao giờ trống, nên chẳng cần Valid @NotBlank
    UserResponse user // chắc chắn không null trong service vì có Validation
) {
}
