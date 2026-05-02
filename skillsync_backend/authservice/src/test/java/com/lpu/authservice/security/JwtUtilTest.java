package com.lpu.authservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inject @Value fields manually
        ReflectionTestUtils.setField(jwtUtil, "SECRET", "KJH78sd7f6sdF87sd6f8SDF7sdf87sdf87sd6f");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
        jwtUtil.init(); // trigger @PostConstruct
    }

    @Test
    void testGenerateToken_NotNull() {
        String token = jwtUtil.generateToken(1L, "user@gmail.com", "ROLE_USER");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void testExtractEmail() {
        String token = jwtUtil.generateToken(1L, "user@gmail.com", "ROLE_USER");
        assertEquals("user@gmail.com", jwtUtil.extractEmail(token));
    }

    @Test
    void testExtractRole() {
        String token = jwtUtil.generateToken(1L, "user@gmail.com", "ROLE_ADMIN");
        assertEquals("ROLE_ADMIN", jwtUtil.extractRole(token));
    }

    @Test
    void testGenerateToken_DifferentRoles() {
        String mentorToken = jwtUtil.generateToken(2L, "mentor@gmail.com", "ROLE_MENTOR");
        assertEquals("ROLE_MENTOR", jwtUtil.extractRole(mentorToken));
        assertEquals("mentor@gmail.com", jwtUtil.extractEmail(mentorToken));
    }

    @Test
    void testInvalidToken_ThrowsException() {
        assertThrows(Exception.class, () -> jwtUtil.extractEmail("invalid.token.here"));
    }
}
