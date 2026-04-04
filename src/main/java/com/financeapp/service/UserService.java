package com.financeapp.service;

import com.financeapp.dto.request.UpdateRoleRequest;
import com.financeapp.dto.request.UpdateStatusRequest;
import com.financeapp.dto.response.UserResponse;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.mapper.UserMapper;
import com.financeapp.model.User;
import com.financeapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = findUserById(id);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateRole(Long id, UpdateRoleRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser.getId().equals(id)) {
            throw new IllegalArgumentException("Cannot modify your own role");
        }
        User user = findUserById(id);
        user.setRole(request.getRole());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateStatus(Long id, UpdateStatusRequest request) {
        User currentUser = getCurrentUser();
        if (currentUser.getId().equals(id)) {
            throw new IllegalArgumentException("Cannot modify your own status");
        }
        User user = findUserById(id);
        user.setStatus(request.getStatus());
        return userMapper.toResponse(userRepository.save(user));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
