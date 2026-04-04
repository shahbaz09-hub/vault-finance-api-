package com.financeapp.service;

import com.financeapp.dto.request.LoginRequest;
import com.financeapp.dto.request.RegisterRequest;
import com.financeapp.dto.response.AuthResponse;
import com.financeapp.dto.response.UserResponse;
import com.financeapp.enums.Role;
import com.financeapp.enums.UserStatus;
import com.financeapp.exception.DuplicateResourceException;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.mapper.UserMapper;
import com.financeapp.model.User;
import com.financeapp.repository.UserRepository;
import com.financeapp.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service class responsible for handling user authentication and registration.
 * 
 * This service encapsulates the business logic for creating new users,
 * validating credentials during login, and managing JWT token generation.
 * It integrates deeply with Spring Security components to ensure secure access.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    /**
     * Registers a new user in the system.
     * 
     * Validates if the email is already in use, encodes the plain-text password,
     * assigns a default 'VIEWER' role, and persists the user to the database.
     * Upon successful registration, a JWT token is immediately issued.
     *
     * @param request The registration details containing name, email, and password.
     * @return AuthResponse containing the user details and generated JWT token.
     * @throws DuplicateResourceException if the email already exists in the system.
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.VIEWER)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        String token = generateTokenForUser(savedUser.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userMapper.toResponse(savedUser))
                .build();
    }

    /**
     * Authenticates an existing user.
     * 
     * Uses Spring Security's AuthenticationManager to validate the credentials.
     * If valid, it retrieves the user from the database and generates a fresh JWT token.
     *
     * @param request The login details containing email and password.
     * @return AuthResponse containing the user details and generated JWT token.
     * @throws ResourceNotFoundException if the user does not exist.
     * @throws org.springframework.security.core.AuthenticationException if credentials are invalid.
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        String token = generateTokenForUser(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userMapper.toResponse(user))
                .build();
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     * 
     * Extracts the user's email from the active SecurityContext and fetches
     * the corresponding User entity from the database.
     *
     * @return UserResponse containing the authenticated user's profile data.
     * @throws ResourceNotFoundException if the user is not found in the database.
     */
    public UserResponse getCurrentUserProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }

    private String generateTokenForUser(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return jwtUtil.generateToken(userDetails);
    }
}
