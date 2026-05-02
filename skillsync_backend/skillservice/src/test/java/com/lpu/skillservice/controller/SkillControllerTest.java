package com.lpu.skillservice.controller;

import com.lpu.skillservice.dto.SkillResponseDTO;
import com.lpu.skillservice.security.JwtFilter;
import com.lpu.skillservice.service.SkillService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SkillController.class)
@AutoConfigureMockMvc(addFilters = false)
class SkillControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private SkillService service;
    @MockBean private JwtFilter jwtFilter;

    // ✅ GET /skills → 200
    @Test
    @WithMockUser
    void testGetAllSkills_Returns200() throws Exception {
        SkillResponseDTO s1 = SkillResponseDTO.builder().id(1L).name("Java").description("Core Java").build();
        SkillResponseDTO s2 = SkillResponseDTO.builder().id(2L).name("Spring").description("Spring Boot").build();

        when(service.getAllSkills()).thenReturn(List.of(s1, s2));

        mockMvc.perform(get("/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[1].name").value("Spring"));
    }

    // ✅ GET /skills → 200 single item
    @Test
    @WithMockUser
    void testGetAllSkills_SingleItem() throws Exception {
        SkillResponseDTO s = SkillResponseDTO.builder().id(1L).name("Docker").description("Containerization").build();
        when(service.getAllSkills()).thenReturn(List.of(s));

        mockMvc.perform(get("/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
