package com.taskmanagement.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.auth.AccessInfo;
import com.taskmanagement.dto.auth.AuthResponse;
import com.taskmanagement.dto.auth.GoogleLinkRequest;
import com.taskmanagement.dto.auth.GoogleLoginRequest;
import com.taskmanagement.dto.auth.LoginRequest;
import com.taskmanagement.dto.auth.RegisterRequest;
import com.taskmanagement.exception.InvalidRefreshTokenException;
import com.taskmanagement.service.auth.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "refresh-token";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";
    private static final String SAME_SITE_POLICY = "Lax";

    private final AuthService authService;

    @Value ("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;
    public AuthController(AuthService authSevice){
        this.authService = authSevice;
    }

    private void setRefreshCookie(String value, HttpServletResponse response) {
        response.addCookie(buildRefreshCookie(
                value,
                Math.toIntExact(refreshExpiration / 1000)
        ));
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addCookie(buildRefreshCookie("", 0));
    }

    private Cookie buildRefreshCookie(String value, int maxAge) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath(REFRESH_COOKIE_PATH);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", SAME_SITE_POLICY);
        return cookie;
    }

    private String getRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @PostMapping("refresh")
    public ResponseEntity<Response<AuthResponse>> refresh(HttpServletRequest request, HttpServletResponse response){
        String refreshToken = getRefreshToken(request);
        if(refreshToken == null){
            return ResponseEntity.status(401).body(Response.error("Refresh token not found!"));
        }

        try {
            AccessInfo accessInfo = authService.refresh(refreshToken);
            setRefreshCookie(accessInfo.refreshToken(), response);
            return ResponseEntity.ok(Response.success(
                    new AuthResponse(accessInfo.accessToken(), accessInfo.user()),
                    "Refresh successful!"
            ));
        } catch (InvalidRefreshTokenException ex) {
            clearRefreshCookie(response);
            throw ex;
        }
    }

    @PostMapping("login")
    public Response<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AccessInfo accessInfo = authService.login(request);
        setRefreshCookie(accessInfo.refreshToken(), response);
        return Response.success(new AuthResponse(accessInfo.accessToken(), accessInfo.user()), "Login successful!");
    }

    @PostMapping("register")
    public Response<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AccessInfo accessInfo = authService.register(request);
        setRefreshCookie(accessInfo.refreshToken(), response);
        return Response.success(new AuthResponse(accessInfo.accessToken(), accessInfo.user()), "Register successful!");
    }

    @PostMapping("google")
    public Response<AuthResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse response
    ) {
        AccessInfo accessInfo = authService.loginWithGoogle(request);
        setRefreshCookie(accessInfo.refreshToken(), response);
        return Response.success(
                new AuthResponse(accessInfo.accessToken(), accessInfo.user()),
                "Google login successful!"
        );
    }

    @PostMapping("google/link")
    public Response<AuthResponse> confirmGoogleLink(
            @Valid @RequestBody GoogleLinkRequest request,
            HttpServletResponse response
    ) {
        AccessInfo accessInfo = authService.confirmGoogleLink(request);
        setRefreshCookie(accessInfo.refreshToken(), response);
        return Response.success(
                new AuthResponse(accessInfo.accessToken(), accessInfo.user()),
                "Google account linked successfully!"
        );
    }

    @PostMapping("logout")
    public Response<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            authService.logout(getRefreshToken(request));
        } finally {
            clearRefreshCookie(response);
        }
        return Response.success(null, "Logout successful!");
    }
}
