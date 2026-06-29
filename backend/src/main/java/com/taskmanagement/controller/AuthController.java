package com.taskmanagement.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.auth.AccessInfo;
import com.taskmanagement.dto.auth.AuthResponse;
import com.taskmanagement.dto.auth.LoginRequest;
import com.taskmanagement.dto.auth.RegisterRequest;
import com.taskmanagement.service.auth.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;




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

    @PostMapping("refresh")
    public ResponseEntity<Response<AuthResponse>> refresh(HttpServletRequest request, HttpServletResponse response){
        Cookie[] cookies = request.getCookies();
        String refreshToken = null;

        if (cookies != null){
            for(Cookie cookie: cookies){
                if ("refresh-token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        if(refreshToken == null){
            return ResponseEntity.status(401).body(Response.error("Refresh token not found!"));
        }

        AccessInfo accessInfo = authService.refresh(refreshToken);
        setCookie("refreshToken", accessInfo.refreshToken(), response);
        return ResponseEntity.ok(Response.success(new AuthResponse(accessInfo.accessToken(), accessInfo.user()), "Refresh successful!"));
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
