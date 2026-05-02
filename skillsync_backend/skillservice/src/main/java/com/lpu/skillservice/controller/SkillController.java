package com.lpu.skillservice.controller;

import com.lpu.skillservice.dto.SkillResponseDTO;
import com.lpu.skillservice.service.SkillService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/skills")
@Tag(name = "Skills", description = "Browse available skills (public)")
public class SkillController {

    @Autowired
    private SkillService service;

    @Operation(
        summary = "Get all skills",
        description = "Public endpoint — lists all skills. No token needed.",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of skills"),
            @ApiResponse(responseCode = "404", description = "No skills available yet")
        }
    )
    @GetMapping
    public ResponseEntity<List<SkillResponseDTO>> getAllSkills() {
        return ResponseEntity.ok(service.getAllSkills());
    }
}
