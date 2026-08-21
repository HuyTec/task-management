package com.taskmanagement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.auth.AccessInfo;
import com.taskmanagement.dto.auth.AuthResponse;
import com.taskmanagement.dto.auth.GoogleLoginRequest;
import com.taskmanagement.dto.auth.LoginRequest;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.exception.InvalidRefreshTokenException;
import com.taskmanagement.model.UserRole;
import com.taskmanagement.service.auth.AuthService;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final long REFRESH_EXPIRATION_MILLIS = 604_800_000L;

    @Mock private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
        ReflectionTestUtils.setField(
                authController,
                "refreshExpiration",
                REFRESH_EXPIRATION_MILLIS
        );
        ReflectionTestUtils.setField(authController, "cookieSecure", true);
    }

    @Test
    void loginSetsRefreshCookieWithProductionSecurityContract() {
        LoginRequest request = new LoginRequest("alice", "Password123!");
        UserResponse user = userResponse();
        when(authService.login(request))
                .thenReturn(new AccessInfo("access-token", "refresh-token", user));
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        Response<AuthResponse> result = authController.login(request, servletResponse);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(new AuthResponse("access-token", user));
        assertRefreshCookie(servletResponse.getCookie("refresh-token"), "refresh-token", 604_800);
    }

    @Test
    void googleLoginSetsTheSameApplicationRefreshCookie() {
        GoogleLoginRequest request = new GoogleLoginRequest("google-credential");
        UserResponse user = userResponse();
        when(authService.loginWithGoogle(request))
                .thenReturn(new AccessInfo("access-token", "refresh-token", user));
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        Response<AuthResponse> result = authController.googleLogin(request, servletResponse);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(new AuthResponse("access-token", user));
        assertRefreshCookie(servletResponse.getCookie("refresh-token"), "refresh-token", 604_800);
    }

    @Test
    void refreshWithoutCookieReturnsUnauthorizedWithoutCallingService() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<Response<AuthResponse>> result = authController.refresh(request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getBody()).isEqualTo(Response.error("Refresh token not found!"));
        verifyNoInteractions(authService);
    }

    @Test
    void rejectedRefreshClearsBrowserCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh-token", "reused-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authService.refresh("reused-token")).thenThrow(new InvalidRefreshTokenException());

        assertThatThrownBy(() -> authController.refresh(request, response))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertRefreshCookie(response.getCookie("refresh-token"), "", 0);
    }

    @Test
    void logoutRevokesServerSessionAndAlwaysClearsBrowserCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh-token", "refresh-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        Response<Void> result = authController.logout(request, response);

        assertThat(result.success()).isTrue();
        verify(authService).logout("refresh-token");
        assertRefreshCookie(response.getCookie("refresh-token"), "", 0);
    }

    private void assertRefreshCookie(Cookie cookie, String value, int maxAge) {
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo(value);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getMaxAge()).isEqualTo(maxAge);
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
    }

    private UserResponse userResponse() {
        return new UserResponse(
                7L,
                "alice",
                "Alice",
                "alice@example.com",
                null,
                UserRole.USER,
                false,
                null,
                null
        );
    }
}
