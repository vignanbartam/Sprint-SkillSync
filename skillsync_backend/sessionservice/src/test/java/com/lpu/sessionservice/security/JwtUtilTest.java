package com.lpu.sessionservice.security;

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
    private String makeToken(String role) {
        return Jwts.builder()
                .setSubject("user@gmail.com")
                .claim("role", role)
                .claim("userId", 1L)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000L))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void testGetRole_User() {
        String token = makeToken("ROLE_USER");
        assertEquals("ROLE_USER", jwtUtil.getRole(token));
    }

    @Test
    void testGetRole_Mentor() {
        String token = makeToken("ROLE_MENTOR");
        assertEquals("ROLE_MENTOR", jwtUtil.getRole(token));
    }

    @Test
    void testGetRole_Admin() {
        String token = makeToken("ROLE_ADMIN");
        assertEquals("ROLE_ADMIN", jwtUtil.getRole(token));
    }

    @Test
    void testValidate_ReturnsClaimsWithEmail() {
        String token = makeToken("ROLE_USER");
        assertNotNull(jwtUtil.validate(token));
        assertEquals("user@gmail.com", jwtUtil.validate(token).getSubject());
    }

    @Test
    void testInvalidToken_ThrowsException() {
        assertThrows(Exception.class, () -> jwtUtil.validate("bad.token.value"));
    }
}
