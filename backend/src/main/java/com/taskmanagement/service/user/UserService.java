package com.taskmanagement.service.user;
import org.springframework.stereotype.Service;
import com.taskmanagement.dto.Response;
import com.taskmanagement.model.User;
import com.taskmanagement.model.UserRole;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.mapper.UserMapper;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.dto.user.CreateUserRequest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.taskmanagement.exception.DuplicatedResourceException;
import com.taskmanagement.exception.ForbiddenException;
import com.taskmanagement.exception.ResourceNotFoundException;
import com.taskmanagement.exception.BadRequestException;
import com.taskmanagement.dto.user.UpdateUserRequest;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
//_____________________________________________UTILS______________________________________________________________

    private void validateUserRequest(CreateUserRequest request) {
        ensureUsernameAvailable(request.username());
        ensureEmailAvailable(request.email());
    }

    private User findUserOrThrow(Long id) {
        if (id == null) {
            throw new BadRequestException("User id is required");
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private User findUserOrThrow(String username) { // overloading
        if (username == null || username.isBlank()) {
            throw new BadRequestException("User id is required");
        }

        return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicatedResourceException("Email already exists: " + email);
        }
    }

    private void ensureUsernameAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicatedResourceException("Username already exists: " + username);
        }
    }

    private User updateUser(User user, UpdateUserRequest request) {
        updateUsernameIfChanged(user, request);
        updateEmailIfChanged(user, request);
        updateDisplayNameIfChanged(user, request);
        updatePasswordIfChanged(user, request);
        return userRepository.save(user);
    }

    private void updateUsernameIfChanged(User user, UpdateUserRequest request) {
        if (request.username() == null || request.username().equals(user.getUsername())) return;

        ensureUsernameAvailable(request.username());
        user.setUsername(request.username());
    }

    private void updateEmailIfChanged(User user, UpdateUserRequest request) {
        if (request.email() == null ||request.email().equals(user.getEmail())) return;

        ensureEmailAvailable(request.email());
        user.setEmail(request.email());
    }

    private void updateDisplayNameIfChanged(User user, UpdateUserRequest request) {
        if (request.displayName() == null) return;
        user.setDisplayName(request.displayName());
    }

    private void updatePasswordIfChanged(User user, UpdateUserRequest request) {
        if (request.password() == null || request.password().isBlank()) return;
        user.setPassword(passwordEncoder.encode(request.password()));
    }

    private Response<UserResponse> saveAndReturn(User user, String message) {
        User savedUser = userRepository.save(user);
        UserResponse userResponse = userMapper.toUserResponse(savedUser);
        return Response.success(userResponse, message);
    }

//_________________________________________________________________________________________________________________
    public Response<List<UserResponse>> getAllUsers() {
        List<User> users =  userRepository.findAll();
        List<UserResponse> userResponses = new ArrayList<>();
        for(User user: users){
            UserResponse userResponse = userMapper.toUserResponse(user);
            userResponses.add(userResponse);
        }
        return Response.success(userResponses, "All user retrieved successfully!");
    }

    public Response<UserResponse> createUser(CreateUserRequest user) {
        validateUserRequest(user);
        
        User userEntity = userMapper.toUser(user); // Use the mapper to convert the request to a User entity, no need to set fields manually

        userEntity.setPassword(passwordEncoder.encode(user.password()));
        userEntity.setRole(UserRole.USER);         // Set a default role, adjust as needed

        return saveAndReturn(userEntity, "User created successfully!");
    }

    public Response<UserResponse> getUserById(Long id) {
        User user = findUserOrThrow(id);
        UserResponse userResponse = userMapper.toUserResponse(user);
        return Response.success(userResponse, "User retrieved successfully!");
    }

    public Response<UserResponse> getUserByUsername(String username) {
        User user = findUserOrThrow(username);
        UserResponse userResponse = userMapper.toUserResponse(user);
        return Response.success(userResponse, "User retrieved successfully!");
    }

    public Response<Void> deleteUserById(Long id) {
        User user = findUserOrThrow(id);
        String currentUsername = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();

        if (user.getUsername().equals(currentUsername)) {
            throw new ForbiddenException("You cannot delete your own account.");
        }
        
        userRepository.delete(user);
        return Response.success(null, "User deleted successfully!");
    }

    public Response<UserResponse> updateUserById(Long id, UpdateUserRequest userRequest) {
        User user = findUserOrThrow(id);
        User savedUser = updateUser(user, userRequest);
        UserResponse userResponse = userMapper.toUserResponse(savedUser);
        return Response.success(userResponse, "User updated successfully!");
    }

    public Response<UserResponse> updateUserByUsername(String username, UpdateUserRequest userRequest) {
        User user = findUserOrThrow(username);
        User savedUser = updateUser(user, userRequest);
        UserResponse userResponse = userMapper.toUserResponse(savedUser);
        return Response.success(userResponse, "User updated successfully!");
    }

    public Response<UserResponse> deactivateUser(Long id) {
        User user = findUserOrThrow(id);
        user.deactivate();
        return saveAndReturn(user, "User deactivated!");
    }

    public Response<UserResponse> activateUser(Long id) {
        User user = findUserOrThrow(id);
        user.activate();
        return saveAndReturn(user, "User activated!");
    }
}
