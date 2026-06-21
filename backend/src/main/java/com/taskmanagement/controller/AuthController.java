package com.taskmanagement.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.auth.AuthResponse;
import com.taskmanagement.dto.auth.LoginRequest;
import com.taskmanagement.dto.auth.RegisterRequest;
import com.taskmanagement.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;



@RestController
@RequestMapping("api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authSevice){
        this.authService = authSevice;
    }

    @PostMapping("login")
    public Response<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("register")
    public Response<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
    
}
