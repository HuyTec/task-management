package com.taskmanagement.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.auth.AccessInfo;
import com.taskmanagement.dto.auth.AuthResponse;
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

    private final AuthService authService;

    @Value ("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;
    public AuthController(AuthService authSevice){
        this.authService = authSevice;
    }

    private void setRefreshCookie(String value, HttpServletResponse response){
        Cookie cookie = new Cookie(REFRESH_COOKIE, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/" );
        cookie.setMaxAge(Math.toIntExact(refreshExpiration / 1000));
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
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
