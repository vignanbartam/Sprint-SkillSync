package com.lpu.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpu.authservice.entity.MentorApplication;
import com.lpu.authservice.repository.UserRepository;
import com.lpu.authservice.security.JwtFilter;
import com.lpu.authservice.service.MentorApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MentorController.class)
@AutoConfigureMockMvc(addFilters = false)
class MentorControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private MentorApplicationService service;
    @MockBean private JwtFilter jwtFilter;
    @MockBean private UserRepository userRepository;

    // ✅ POST /user/mentor-application → 201
    @Test
    void testApply_Returns201() throws Exception {
        MentorApplication app = MentorApplication.builder()
                .userId(1L).skillIds(List.of(1L, 2L)).experience(3).build();

        when(service.apply(any())).thenReturn("Application Submitted");

        mockMvc.perform(post("/user/mentor-application")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(app)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Application Submitted"));
    }
}
