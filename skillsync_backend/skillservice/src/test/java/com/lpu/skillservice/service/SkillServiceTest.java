package com.lpu.skillservice.service;

import org.springframework.test.context.ActiveProfiles;

import com.lpu.skillservice.dto.SkillRequestDTO;
import com.lpu.skillservice.dto.SkillResponseDTO;
import com.lpu.skillservice.entity.Skill;
import com.lpu.skillservice.exception.CustomException;
import com.lpu.skillservice.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock private SkillRepository repo;

    @InjectMocks
    private SkillService service;

    // ✅ Add skill: success
    @Test
    void testAddSkill_Success() {
        SkillRequestDTO dto = new SkillRequestDTO("Java", "Core Java");
        Skill saved = Skill.builder().id(1L).name("Java").description("Core Java").build();

        when(repo.existsByName("Java")).thenReturn(false);
        when(repo.save(any())).thenReturn(saved);

        SkillResponseDTO result = service.addSkill(dto);

        assertEquals("Java", result.getName());
        assertEquals(1L, result.getId());
    }

    // ❌ Add skill: name blank
    @Test
    void testAddSkill_BlankName() {
        SkillRequestDTO dto = new SkillRequestDTO("", "description");
        assertThrows(CustomException.class, () -> service.addSkill(dto));
    }

    // ❌ Add skill: name null
    @Test
    void testAddSkill_NullName() {
        SkillRequestDTO dto = new SkillRequestDTO(null, "description");
        assertThrows(CustomException.class, () -> service.addSkill(dto));
    }

    // ❌ Add skill: duplicate
    @Test
    void testAddSkill_DuplicateName() {
        SkillRequestDTO dto = new SkillRequestDTO("Java", "Core Java");
        when(repo.existsByName("Java")).thenReturn(true);

        assertThrows(CustomException.class, () -> service.addSkill(dto));
        verify(repo, never()).save(any());
    }

    // ✅ Get all skills: success
    @Test
    void testGetAllSkills_Success() {
        Skill s1 = Skill.builder().id(1L).name("Java").build();
        Skill s2 = Skill.builder().id(2L).name("Spring").build();
        when(repo.findAll()).thenReturn(List.of(s1, s2));

        List<SkillResponseDTO> result = service.getAllSkills();

        assertEquals(2, result.size());
    }

    // ❌ Get all skills: empty
    @Test
    void testGetAllSkills_Empty() {
        when(repo.findAll()).thenReturn(List.of());
        assertThrows(CustomException.class, () -> service.getAllSkills());
    }

    // ✅ Delete skill: success
    @Test
    void testDeleteSkill_Success() {
        Skill skill = Skill.builder().id(1L).name("Java").build();
        when(repo.findById(1L)).thenReturn(Optional.of(skill));

        String result = service.deleteSkill(1L);

        assertEquals("Skill deleted successfully", result);
        verify(repo, times(1)).delete(skill);
    }

    // ❌ Delete skill: not found
    @Test
    void testDeleteSkill_NotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(CustomException.class, () -> service.deleteSkill(99L));
    }
}
