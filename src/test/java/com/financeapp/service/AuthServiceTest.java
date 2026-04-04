package com.financeapp.service;

import com.financeapp.dto.request.LoginRequest;
import com.financeapp.dto.request.RegisterRequest;
import com.financeapp.dto.response.AuthResponse;
import com.financeapp.dto.response.UserResponse;
import com.financeapp.enums.Role;
import com.financeapp.enums.UserStatus;
import com.financeapp.exception.DuplicateResourceException;
import com.financeapp.mapper.UserMapper;
import com.financeapp.model.User;
import com.financeapp.repository.UserRepository;
import com.financeapp.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UserResponse testUserResponse;
    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@test.com")
                .password("encodedPassword")
                .role(Role.VIEWER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        testUserResponse = UserResponse.builder()
                .id(1L)
                .name("John Doe")
                .email("john@test.com")
                .role(Role.VIEWER)
                .status(UserStatus.ACTIVE)
                .createdAt(testUser.getCreatedAt())
                .build();

        testUserDetails = new org.springframework.security.core.userdetails.User(
                "john@test.com", "encodedPassword",
                List.of(new SimpleGrantedAuthority("ROLE_VIEWER"))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Register - should create user with VIEWER role and return token")
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@test.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userDetailsService.loadUserByUsername("john@test.com")).thenReturn(testUserDetails);
        when(jwtUtil.generateToken(testUserDetails)).thenReturn("test-jwt-token");
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("test-jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getRole()).isEqualTo(Role.VIEWER);
        assertThat(response.getUser().getEmail()).isEqualTo("john@test.com");

        verify(userRepository).existsByEmail("john@test.com");
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("Register - should throw DuplicateResourceException for existing email")
    void register_DuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("existing@test.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login - should authenticate and return token")
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@test.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("john@test.com")).thenReturn(testUserDetails);
        when(jwtUtil.generateToken(testUserDetails)).thenReturn("login-jwt-token");
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("login-jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo("john@test.com");

        verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("Login - should throw BadCredentialsException for wrong password")
    void login_BadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@test.com");
        request.setPassword("wrongPassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Get current user profile - should return UserResponse")
    void getCurrentUserProfile_Success() {
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("john@test.com");
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        UserResponse response = authService.getCurrentUserProfile();

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john@test.com");
        assertThat(response.getName()).isEqualTo("John Doe");

        verify(userRepository).findByEmail("john@test.com");
    }
}
