package com.lpu.skillservice.controller;

import com.lpu.skillservice.dto.SkillRequestDTO;
import com.lpu.skillservice.dto.SkillResponseDTO;
import com.lpu.skillservice.service.SkillService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/admin/skills")
@Tag(name = "Admin — Skills", description = "Create and delete skills (requires ROLE_ADMIN JWT)")
public class AdminSkillController {

    @Autowired
    private SkillService service;

    @Operation(
        summary = "Create a new skill",
        description = "Adds a skill to the catalogue. Requires ADMIN token.",
        security = @SecurityRequirement(name = "BearerAuth"),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"name\":\"Spring Boot\",\"description\":\"Java microservices framework\"}")
            )
        ),
        responses = {
            @ApiResponse(responseCode = "201", description = "Skill created"),
            @ApiResponse(responseCode = "400", description = "Skill already exists / name blank"),
            @ApiResponse(responseCode = "403", description = "Access denied — not ADMIN")
        }
    )
    @PostMapping
    public ResponseEntity<SkillResponseDTO> addSkill(@RequestBody SkillRequestDTO dto) {
        return ResponseEntity.status(201).body(service.addSkill(dto));
    }

    @Operation(
        summary = "Delete a skill",
        description = "Removes a skill by ID. Requires ADMIN token.",
        security = @SecurityRequirement(name = "BearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Skill deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied — not ADMIN"),
            @ApiResponse(responseCode = "404", description = "Skill not found")
        }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSkill(
            @Parameter(description = "Skill ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(service.deleteSkill(id));
    }
}
