package com.taskmanagement.service.auth;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.taskmanagement.dto.auth.LoginRequest;
import com.taskmanagement.dto.auth.RegisterRequest;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.mapper.UserMapper;
import com.taskmanagement.model.User;
import com.taskmanagement.dto.auth.AuthResponse;
import com.taskmanagement.dto.Response;
import com.taskmanagement.repository.UserRepository;

import jakarta.validation.Valid;

@Service
@Validated
public class AuthService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final AuthenticationManager authManager;

    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authManager, UserMapper userMapper){
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    public Response<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        String username = request.username();
        String password = request.password();
        
        authManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

        User user =  userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found " + username));

        String accessToken = jwtService.generateToken(username);
        String refreshToken = jwtService.generateRefreshToken(username);
        UserResponse userResponse = userMapper.toUserResponse(user);

        AuthResponse authResponse = new AuthResponse(accessToken, refreshToken, userResponse);
        return Response.success(authResponse, "Login successful!");
    }

    public Response<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        String displayName = request.displayName();
        String username = request.username();
        String password = request.password();
        String email = request.email();

        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            throw new RuntimeException("Username or Email is existed");
        }

        User user = new User();
        user.setDisplayName(displayName);
        if (user.getDisplayName() == null || user.getDisplayName().isBlank()) {
            user.setDisplayName(username);
        }
        
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);

        String accessToken = jwtService.generateToken(username);
        String refreshToken = jwtService.generateRefreshToken(username);

        UserResponse userResponse = userMapper.toUserResponse(user);
        return Response.success(new AuthResponse(accessToken, refreshToken, userResponse),"Register successful!");
    }

}
