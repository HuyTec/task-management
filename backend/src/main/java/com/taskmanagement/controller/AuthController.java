package com.taskmanagement.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.auth.AccessInfo;
import com.taskmanagement.dto.auth.AuthResponse;
import com.taskmanagement.dto.auth.LoginRequest;
import com.taskmanagement.dto.auth.RegisterRequest;
import com.taskmanagement.service.auth.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;



@RestController
@RequestMapping("api/auth")
public class AuthController {
    private final AuthService authService;

    @Value ("${jwt.refresh-expiration}")
    private int refreshExpiration;

    public AuthController(AuthService authSevice){
        this.authService = authSevice;
    }

    private void setCookie(String name, String value, HttpServletResponse response){
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/" );
        cookie.setMaxAge(refreshExpiration);
        response.addCookie(cookie);
    }

    @PostMapping("login")
    public Response<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AccessInfo accessInfo = authService.login(request);
        setCookie("refresh-token", accessInfo.refreshToken(), response);
        return Response.success(new AuthResponse(accessInfo.accessToken(), accessInfo.user()), "Login successful!");
    }

    @PostMapping("register")
    public Response<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AccessInfo accessInfo = authService.register(request);
        setCookie("refresh-token", accessInfo.refreshToken(), response);
        return Response.success(new AuthResponse(accessInfo.accessToken(), accessInfo.user()), "Register successful!");
    }
}
