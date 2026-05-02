package com.lpu.groupservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpu.groupservice.config.HeaderAuthFilter;
import com.lpu.groupservice.config.UserContext;
import com.lpu.groupservice.dto.GroupRequestDTO;
import com.lpu.groupservice.dto.GroupResponseDTO;
import com.lpu.groupservice.service.GroupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class GroupControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private GroupService service;
    @MockBean private UserContext userContext;
    @MockBean private HeaderAuthFilter headerAuthFilter;

    private GroupResponseDTO buildResponse(Long id) {
        return GroupResponseDTO.builder()
                .id(id).name("Dev Group").description("For devs").createdBy(10L).build();
    }

    // ✅ POST /groups → 201
    @Test
    @WithMockUser
    void testCreate_Returns201() throws Exception {
        GroupRequestDTO dto = new GroupRequestDTO();
        dto.setName("Dev Group");
        dto.setDescription("For devs");

        when(userContext.getUserId(any(HttpServletRequest.class))).thenReturn(10L);
        when(service.create(any(), eq(10L))).thenReturn(buildResponse(1L));

        mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Dev Group"))
                .andExpect(jsonPath("$.createdBy").value(10));
    }

    // ✅ GET /groups → 200
    @Test
    @WithMockUser
    void testGetAllGroups_Returns200() throws Exception {
        when(service.getAllGroups()).thenReturn(List.of(buildResponse(1L), buildResponse(2L)));

        mockMvc.perform(get("/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ✅ GET /groups → 200 empty
    @Test
    @WithMockUser
    void testGetAllGroups_Empty() throws Exception {
        when(service.getAllGroups()).thenReturn(List.of());

        mockMvc.perform(get("/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ✅ POST /groups/{id}/join → 200
    @Test
    @WithMockUser
    void testJoin_Returns200() throws Exception {
        when(userContext.getUserId(any(HttpServletRequest.class))).thenReturn(5L);
        when(service.join(1L, 5L)).thenReturn("Joined group");

        mockMvc.perform(post("/groups/1/join")
                        )
                .andExpect(status().isOk())
                .andExpect(content().string("Joined group"));
    }

    // ✅ PUT /groups/{id}/approve/{userId} → 200
    @Test
    @WithMockUser
    void testApprove_Returns200() throws Exception {
        when(userContext.getUserId(any(HttpServletRequest.class))).thenReturn(10L);
        when(userContext.getRole(any(HttpServletRequest.class))).thenReturn("");
        when(service.approve(1L, 10L, "", 5L)).thenReturn("Approved");

        mockMvc.perform(put("/groups/1/approve/5")
                        )
                .andExpect(status().isOk())
                .andExpect(content().string("Approved"));
    }

    // ✅ DELETE /groups/{id}/members/{userId} → 200
    @Test
    @WithMockUser
    void testRemove_Returns200() throws Exception {
        when(userContext.getUserId(any(HttpServletRequest.class))).thenReturn(10L);
        when(userContext.getRole(any(HttpServletRequest.class))).thenReturn("");
        when(service.remove(1L, 10L, "", 5L)).thenReturn("Removed");

        mockMvc.perform(delete("/groups/1/members/5")
                        )
                .andExpect(status().isOk())
                .andExpect(content().string("Removed"));
    }
}
