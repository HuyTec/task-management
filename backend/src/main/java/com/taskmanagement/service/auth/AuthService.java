package com.taskmanagement.service.auth;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import com.taskmanagement.dto.auth.AccessInfo;
import com.taskmanagement.dto.auth.LoginRequest;
import com.taskmanagement.dto.auth.RefreshTokenClaims;
import com.taskmanagement.dto.auth.RegisterRequest;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.exception.DuplicatedResourceException;
import com.taskmanagement.exception.InvalidRefreshTokenException;
import com.taskmanagement.mapper.UserMapper;
import com.taskmanagement.model.User;
import com.taskmanagement.model.UserRole;
import com.taskmanagement.repository.UserRepository;

import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;

@Service
@Validated
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final UserMapper userMapper;
    private final AuthSessionService authSessionService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authManager,
            UserMapper userMapper,
            AuthSessionService authSessionService
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.authSessionService = authSessionService;
    }

    public AccessInfo login(@Valid LoginRequest request) {
        String username = request.username();

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.password())
        );

        

        User user = userRepository.findByUsernameAndIsDeactivatedFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found " + username));

        return issueSession(user, UUID.randomUUID().toString());
    }

    @Transactional
    public AccessInfo register(@Valid RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicatedResourceException("Username is existed");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicatedResourceException("Email is existed");
        }

        User user = new User();
        user.setDisplayName(request.displayName());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);

        userRepository.save(user);
        return issueSession(user, UUID.randomUUID().toString());
    }

    public AccessInfo refresh(String refreshToken) {
        RefreshTokenClaims claims;
        try {
            claims = jwtService.parseRefreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findByUsernameAndIsDeactivatedFalse(claims.username())
                .orElseThrow(InvalidRefreshTokenException::new);

        String newTokenId = UUID.randomUUID().toString();
        boolean rotated = authSessionService.rotate(
                claims.sessionId(),
                claims.tokenId(),
                newTokenId
        );
        if (!rotated) {
            throw new InvalidRefreshTokenException();
        }

        String accessToken = jwtService.generateAccessToken(claims.username(), claims.sessionId());
        String newRefreshToken = jwtService.generateRefreshToken(
                claims.username(),
                claims.sessionId(),
                newTokenId
        );
        UserResponse userResponse = userMapper.toUserResponse(user);

        return new AccessInfo(accessToken, newRefreshToken, userResponse);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        try {
            RefreshTokenClaims claims = jwtService.parseRefreshToken(refreshToken);
            authSessionService.revoke(claims.sessionId());
        } catch (JwtException | IllegalArgumentException ignored) {
            // The client cookie is still cleared by the controller.
        }
    }

    private AccessInfo issueSession(User user, String sessionId) {
        String tokenId = UUID.randomUUID().toString();
        String accessToken = jwtService.generateAccessToken(user.getUsername(), sessionId);
        String refreshToken = jwtService.generateRefreshToken(
                user.getUsername(),
                sessionId,
                tokenId
        );

        authSessionService.create(sessionId, tokenId);

        UserResponse userResponse = userMapper.toUserResponse(user);
        return new AccessInfo(accessToken, refreshToken, userResponse);
    }
}
