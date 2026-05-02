package com.lpu.reviewservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpu.reviewservice.entity.Review;
import com.lpu.reviewservice.security.JwtFilter;
import com.lpu.reviewservice.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ReviewService service;
    @MockBean private JwtFilter jwtFilter;

    // ✅ POST /reviews → 201
    @Test
    @WithMockUser(roles = "USER")
    void testAddReview_Returns201() throws Exception {
        Review review = new Review(null, 1L, 2L, 5, "Excellent!");
        Review saved  = new Review(1L,  1L, 2L, 5, "Excellent!");

        when(service.addReview(any())).thenReturn(saved);

        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(review)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rating").value(5));
    }

    // ✅ GET /reviews/mentor/{mentorId} → 200
    @Test
    @WithMockUser
    void testGetReviews_Returns200() throws Exception {
        Review r = new Review(1L, 1L, 2L, 4, "Good");
        when(service.getReviews(2L)).thenReturn(List.of(r));

        mockMvc.perform(get("/reviews/mentor/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mentorId").value(2))
                .andExpect(jsonPath("$[0].rating").value(4));
    }

    // ✅ GET /reviews/mentor/{mentorId} → 200 empty list case handled by service
    @Test
    @WithMockUser
    void testGetReviews_EmptyList() throws Exception {
        when(service.getReviews(99L)).thenReturn(List.of());

        mockMvc.perform(get("/reviews/mentor/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
