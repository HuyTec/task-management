package com.taskmanagement.service.user;
import org.springframework.stereotype.Service;
import com.taskmanagement.dto.Response;
import com.taskmanagement.model.User;
import com.taskmanagement.model.UserRole;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.mapper.UserMapper;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.dto.user.CreateUserRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.taskmanagement.exception.DuplicatedResourceException;
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

        return userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
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

//_________________________________________________________________________________________________________________

    public Response<UserResponse> createUser(CreateUserRequest user) {
        validateUserRequest(user);
        
        User userEntity = userMapper.toUser(user); // Use the mapper to convert the request to a User entity, no need to set fields manually

        userEntity.setPassword(passwordEncoder.encode(user.password()));
        userEntity.setRole(UserRole.USER);         // Set a default role, adjust as needed

        userRepository.save(userEntity);
        UserResponse userResponse = userMapper.toUserResponse(userEntity);
        return Response.success(userResponse, "User created successfully!");
    }

    public Response<UserResponse> getUserById(Long id) {
        User user = findUserOrThrow(id);
        UserResponse userResponse = userMapper.toUserResponse(user);
        return Response.success(userResponse, "User retrieved successfully!");
    }

    public Response<Void> deleteUserById(Long id) {
        User user = findUserOrThrow(id);
        user.deactivate(); // Mark the user as deleted instead of actually deleting them
        userRepository.save(user);
        return Response.success(null, "User deleted successfully!");
    }

    public Response<UserResponse> updateUserById(Long id, UpdateUserRequest userRequest) {
        User user = findUserOrThrow(id);

        if (userRequest.username() != null && !userRequest.username().equals(user.getUsername())) {
            ensureUsernameAvailable(userRequest.username());
            user.setUsername(userRequest.username());
        }

        if (userRequest.email() != null && !userRequest.email().equals(user.getEmail())) {
            ensureEmailAvailable(userRequest.email());
            user.setEmail(userRequest.email());
        }

        if (userRequest.displayName() != null) {
            user.setDisplayName(userRequest.displayName());
        }

        if (userRequest.password() != null) {
            user.setPassword(passwordEncoder.encode(userRequest.password()));
        }

        User savedUser = userRepository.save(user);
        UserResponse userResponse = userMapper.toUserResponse(savedUser);
        return Response.success(userResponse, "User updated successfully!");
    }
}
