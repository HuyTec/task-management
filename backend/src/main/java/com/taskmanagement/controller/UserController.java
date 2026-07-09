package com.taskmanagement.controller;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.taskmanagement.dto.Response;
import com.taskmanagement.dto.user.UpdateUserRequest;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.service.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ADMIN ONLY — xem toàn bộ user trong hệ thống
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<List<UserResponse>>> getAllUsers() {
        Response<List<UserResponse>> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // ADMIN ONLY — xem chi tiết 1 user bất kỳ theo id
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<UserResponse>> getUserById(@PathVariable @Positive Long id) {
        Response<UserResponse> user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    // SELF — user tự xem profile của mình, không cần biết id
    @GetMapping("/me")
    public ResponseEntity<Response<UserResponse>> getMyProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Response<UserResponse> user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    // SELF — user tự update profile của mình
    @PatchMapping("/me")
    public ResponseEntity<Response<UserResponse>> updateMyProfile(@RequestBody @Valid UpdateUserRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Response<UserResponse> updated = userService.updateUserByUsername(username, request);
        return ResponseEntity.ok(updated);
    }

    // ADMIN ONLY — hard delete một user
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<Void>> deleteUser(@PathVariable @Positive Long id) {
        Response<Void> deleted = userService.deleteUserById(id);
        return ResponseEntity.ok(deleted);
    }

    // ADMIN ONLY — khôi phục lại user đã bị deactivate
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<UserResponse>> activateUser(@PathVariable @Positive Long id) {
        Response<UserResponse> user = userService.activateUser(id);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response<UserResponse>> deactivateUser(@PathVariable @Positive Long id) {
        Response<UserResponse> user = userService.deactivateUser(id);
        return ResponseEntity.ok(user);
    }
}