package com.lpu.skillservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpu.skillservice.dto.SkillRequestDTO;
import com.lpu.skillservice.dto.SkillResponseDTO;
import com.lpu.skillservice.security.JwtFilter;
import com.lpu.skillservice.service.SkillService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminSkillController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminSkillControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SkillService service;
    @MockBean private JwtFilter jwtFilter;

    // ✅ POST /admin/skills → 201
    @Test
    @WithMockUser(roles = "ADMIN")
    void testAddSkill_Returns201() throws Exception {
        SkillRequestDTO dto = new SkillRequestDTO("Kubernetes", "Container orchestration");
        SkillResponseDTO response = SkillResponseDTO.builder()
                .id(1L).name("Kubernetes").description("Container orchestration").build();

        when(service.addSkill(any())).thenReturn(response);

        mockMvc.perform(post("/admin/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Kubernetes"));
    }

    // ✅ DELETE /admin/skills/{id} → 200
    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteSkill_Returns200() throws Exception {
        when(service.deleteSkill(1L)).thenReturn("Skill deleted successfully");

        mockMvc.perform(delete("/admin/skills/1")
                        )
                .andExpect(status().isOk())
                .andExpect(content().string("Skill deleted successfully"));
    }
}
