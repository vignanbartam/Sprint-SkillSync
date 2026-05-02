package com.lpu.skillservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String SECRET = "KJH78sd7f6sdF87sd6f8SDF7sdf87sdf87sd6f";
    private Key signingKey;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "SECRET", SECRET);
        jwtUtil.init();
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    /** Build a valid JWT token locally — no cross-service dependency */
    private String makeToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("userId", 1L)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000L))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void testExtractEmail() {
        String token = makeToken("admin@gmail.com", "ROLE_ADMIN");
        assertEquals("admin@gmail.com", jwtUtil.extractEmail(token));
    }

    @Test
    void testExtractRole() {
        String token = makeToken("admin@gmail.com", "ROLE_ADMIN");
        assertEquals("ROLE_ADMIN", jwtUtil.extractRole(token));
    }

    @Test
    void testExtractRole_User() {
        String token = makeToken("user@gmail.com", "ROLE_USER");
        assertEquals("ROLE_USER", jwtUtil.extractRole(token));
    }

    @Test
    void testValidateToken_NotNull() {
        String token = makeToken("user@gmail.com", "ROLE_USER");
        assertNotNull(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_CorrectSubject() {
        String token = makeToken("mentor@gmail.com", "ROLE_MENTOR");
        assertEquals("mentor@gmail.com", jwtUtil.validateToken(token).getSubject());
    }

    @Test
    void testInvalidToken_ThrowsException() {
        assertThrows(Exception.class, () -> jwtUtil.validateToken("invalid.jwt.token"));
    }
}
