package com.lpu.sessionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpu.sessionservice.dto.SessionRequestDTO;
import com.lpu.sessionservice.dto.SessionResponseDTO;
import com.lpu.sessionservice.dto.SessionUpdateRequestDTO;
import com.lpu.sessionservice.repository.SessionRepository;
import com.lpu.sessionservice.security.JwtFilter;
import com.lpu.sessionservice.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SessionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SessionService service;
    @MockBean private SessionRepository repo;
    @MockBean private JwtFilter jwtFilter;

    // ✅ POST /session → 201
    @Test
    @WithMockUser(roles = "USER")
    void testBookSession_Returns201() throws Exception {
        SessionRequestDTO dto = new SessionRequestDTO(1L, 2L);
        SessionResponseDTO response = SessionResponseDTO.builder()
                .id(10L).userId(1L).mentorId(2L).status("PENDING").build();

        when(service.getAuthenticatedUserId()).thenReturn(1L);
        when(service.book(any(), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    // ✅ PUT /session/{id}/{status} → 200
    @Test
    @WithMockUser(roles = "MENTOR")
    void testUpdateStatus_Returns200() throws Exception {
        SessionResponseDTO response = SessionResponseDTO.builder()
                .id(1L).userId(1L).mentorId(2L).status("ACCEPTED").build();
        SessionUpdateRequestDTO dto = new SessionUpdateRequestDTO();
        dto.setTimeSlot("2026-05-02T10:00");
        dto.setMeetingUrl("https://meet.google.com/test");

        when(service.getAuthenticatedUserId()).thenReturn(2L);
        when(service.updateStatus(eq(1L), eq("ACCEPTED"), eq(2L), any(SessionUpdateRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/session/1/ACCEPTED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    // ✅ GET /session/check → 200 true
    @Test
    @WithMockUser
    void testCheckCompleted_ReturnsTrue() throws Exception {
        when(repo.existsByUserIdAndMentorIdAndStatus(1L, 2L, "COMPLETED")).thenReturn(true);

        mockMvc.perform(get("/session/check")
                        .param("userId", "1")
                        .param("mentorId", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // ✅ GET /session/check → 200 false
    @Test
    @WithMockUser
    void testCheckCompleted_ReturnsFalse() throws Exception {
        when(repo.existsByUserIdAndMentorIdAndStatus(1L, 2L, "COMPLETED")).thenReturn(false);

        mockMvc.perform(get("/session/check")
                        .param("userId", "1")
                        .param("mentorId", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // ✅ PUT /session/{id}/complete → 200
    @Test
    @WithMockUser(roles = "MENTOR")
    void testCompleteSession_Returns200() throws Exception {
        SessionResponseDTO response = SessionResponseDTO.builder()
                .id(1L).userId(1L).mentorId(2L).status("COMPLETED").build();

        when(service.getAuthenticatedUserId()).thenReturn(2L);
        when(service.updateStatus(1L, "COMPLETED", 2L)).thenReturn(response);

        mockMvc.perform(put("/session/1/complete")
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
