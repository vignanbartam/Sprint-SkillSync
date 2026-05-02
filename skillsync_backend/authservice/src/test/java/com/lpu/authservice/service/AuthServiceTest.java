package com.lpu.authservice.service;

import com.lpu.authservice.entity.User;
import com.lpu.authservice.exception.CustomException;
import com.lpu.authservice.repository.UserRepository;
import com.lpu.authservice.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void testRegister_EmailServiceUnavailable() {
        User user = User.builder()
                .email("test@gmail.com")
                .password("pass123")
                .name("Test User")
                .age(22)
                .build();
        when(userRepo.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(encoder.encode("pass123")).thenReturn("encodedPass");

        CustomException ex = assertThrows(CustomException.class, () -> authService.register(user));
        assertEquals("Unable to send OTP email. Please make sure RabbitMQ and notificationservice are running.", ex.getMessage());
        verify(userRepo, never()).save(any(User.class));
    }

    // ❌ Register: email already exists
    @Test
    void testRegister_EmailAlreadyExists() {
        User user = User.builder()
                .email("test@gmail.com")
                .password("pass123")
                .name("Test User")
                .age(22)
                .build();
        when(userRepo.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));

        assertThrows(CustomException.class, () -> authService.register(user));
    }

    // ✅ Login: success
    @Test
    void testLogin_Success() {
        when(userRepo.findIdByEmail("test@gmail.com")).thenReturn(Optional.of(1L));
        when(userRepo.findPasswordByEmail("test@gmail.com")).thenReturn(Optional.of("encodedPass"));
        when(userRepo.findRoleByEmail("test@gmail.com")).thenReturn(Optional.of("ROLE_USER"));
        when(encoder.matches("pass123", "encodedPass")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "test@gmail.com", "ROLE_USER")).thenReturn("jwt.token.here");

        String token = authService.login("test@gmail.com", "pass123");

        assertEquals("jwt.token.here", token);
    }

    // ❌ Login: wrong password
    @Test
    void testLogin_WrongPassword() {
        when(userRepo.findIdByEmail("test@gmail.com")).thenReturn(Optional.of(1L));
        when(userRepo.findPasswordByEmail("test@gmail.com")).thenReturn(Optional.of("encodedPass"));
        when(userRepo.findRoleByEmail("test@gmail.com")).thenReturn(Optional.of("ROLE_USER"));
        when(encoder.matches("wrongPass", "encodedPass")).thenReturn(false);

        assertThrows(CustomException.class, () -> authService.login("test@gmail.com", "wrongPass"));
    }

    // ❌ Login: user not found
    @Test
    void testLogin_UserNotFound() {
        when(userRepo.findIdByEmail("unknown@gmail.com")).thenReturn(Optional.empty());

        assertThrows(CustomException.class, () -> authService.login("unknown@gmail.com", "pass123"));
    }
}
