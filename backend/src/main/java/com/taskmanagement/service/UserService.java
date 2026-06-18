package com.taskmanagement.service;
import org.springframework.stereotype.Service;
import com.taskmanagement.dto.Response;
import com.taskmanagement.model.User;
import com.taskmanagement.dto.user.UserResponse;
import com.taskmanagement.mapper.UserMapper;
import com.taskmanagement.repository.UserRepository;
import com.taskmanagement.dto.user.CreateUserRequest;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public void saveUser(CreateUserRequest user) {
        User userEntity = userMapper.toUser(user);
        userEntity.setUsername(user.username());
        userEntity.setDisplayName(user.displayName());
        userEntity.setEmail(user.email());
        userEntity.setPassword(user.password());
        userEntity.setRole(user.role());
        userEntity.setCreatedAt();
        userRepository.save(userEntity);
    }
}
