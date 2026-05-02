package com.lpu.skillservice.service;

import com.lpu.skillservice.dto.*;
import com.lpu.skillservice.entity.Skill;
import com.lpu.skillservice.repository.SkillRepository;
import com.lpu.skillservice.exception.CustomException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SkillService {

    @Autowired
    private SkillRepository repo;

    // Add Skill (Admin)
    public SkillResponseDTO addSkill(SkillRequestDTO dto) {

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new CustomException("Skill name is required");
        }

        // Optional: prevent duplicate skill
        if (repo.existsByName(dto.getName())) {
            throw new CustomException("Skill already exists");
        }

        Skill skill = Skill.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();

        Skill saved = repo.save(skill);

        return mapToDTO(saved);
    }

    // Get All Skills
    public List<SkillResponseDTO> getAllSkills() {

        List<Skill> skills = repo.findAll();

        if (skills.isEmpty()) {
            throw new CustomException("No skills available");
        }

        return skills.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Delete Skill
    public String deleteSkill(Long id) {

        Skill skill = repo.findById(id)
                .orElseThrow(() -> new CustomException("Skill not found"));

        repo.delete(skill);

        return "Skill deleted successfully";
    }

    // Mapper
    private SkillResponseDTO mapToDTO(Skill skill) {
        return SkillResponseDTO.builder()
                .id(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription())
                .build();
    }
}