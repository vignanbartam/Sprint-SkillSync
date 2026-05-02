package com.lpu.authservice.controller;

import com.lpu.authservice.entity.User;
import com.lpu.authservice.dto.UserDTO;
import com.lpu.authservice.repository.UserRepository;
import com.lpu.authservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private com.lpu.authservice.security.JwtFilter jwtFilter;

    @Autowired
    private ObjectMapper objectMapper;

    // ✅ POST /auth/register → 201
    @Test
    void testRegister_Returns201() throws Exception {
        User user = User.builder()
                .email("test@gmail.com")
                .password("pass123")
                .name("Test User")
                .age(22)
                .build();
        when(authService.register(any())).thenReturn("Registered successfully");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Registered successfully"));
    }

    // ✅ POST /auth/login → 200
    @Test
    void testLogin_Returns200() throws Exception {
        User user = User.builder()
                .email("test@gmail.com")
                .password("pass123")
                .build();
        when(authService.login("test@gmail.com", "pass123")).thenReturn("jwt.token.here");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(content().string("jwt.token.here"));
    }

    // ✅ GET /auth/user/{id} → 200
    @Test
    void testGetUser_Returns200() throws Exception {
        UserDTO user = UserDTO.builder()
                .id(1L)
                .email("test@gmail.com")
                .role("ROLE_USER")
                .build();
        when(authService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/auth/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@gmail.com"));
    }

    // ✅ GET /auth/user/{id} → 400 when not found
    @Test
    void testGetUser_NotFound() throws Exception {
        when(authService.getUserById(99L))
                .thenThrow(new com.lpu.authservice.exception.CustomException("User not found"));

        mockMvc.perform(get("/auth/user/99"))
                .andExpect(status().isBadRequest());
    }
}
