package com.lpu.authservice.controller;

import com.lpu.authservice.repository.UserRepository;
import com.lpu.authservice.security.JwtFilter;
import com.lpu.authservice.service.AuthService;
import com.lpu.authservice.service.MentorApplicationService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(com.lpu.authservice.security.SecurityConfig.class)
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private MentorApplicationService service;
    @MockBean private AuthService authService;
    @MockBean private JwtFilter jwtFilter;
    @MockBean private UserRepository userRepository;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    // ✅ PUT /admin/mentors/{id}/approve → 200 with ADMIN role
    @Test
    @WithMockUser(roles = "ADMIN")
    void testApprove_Returns200() throws Exception {
        when(service.approve(1L)).thenReturn("Mentor Approved");

        mockMvc.perform(put("/admin/mentors/1/approve"))
                .andExpect(status().isOk())
                .andExpect(content().string("Mentor Approved"));
    }

    // ✅ PUT /admin/mentors/{id}/approve → 403 for non-admin (method-level @PreAuthorize)
    @Test
    @WithMockUser(roles = "USER")
    void testApprove_ForbiddenForUser() throws Exception {
        mockMvc.perform(put("/admin/mentors/1/approve"))
                .andExpect(status().isForbidden());
    }
}
