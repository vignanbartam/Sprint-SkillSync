package com.lpu.reviewservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String SECRET = "KJH78sd7f6sdF87sd6f8SDF7sdf87sdf87sd6f";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "SECRET", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
        jwtUtil.init();
    }

    @Test
    void testGenerateAndExtractEmail() {
        String token = jwtUtil.generateToken("mentor@gmail.com", "ROLE_MENTOR");
        assertEquals("mentor@gmail.com", jwtUtil.extractEmail(token));
    }

    @Test
    void testExtractRole() {
        String token = jwtUtil.generateToken("user@gmail.com", "ROLE_USER");
        assertEquals("ROLE_USER", jwtUtil.extractRole(token));
    }

    @Test
    void testGenerateToken_NotEmpty() {
        String token = jwtUtil.generateToken("a@b.com", "ROLE_USER");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void testInvalidToken_ThrowsException() {
        assertThrows(Exception.class, () -> jwtUtil.extractEmail("not.a.real.token"));
    }
}
