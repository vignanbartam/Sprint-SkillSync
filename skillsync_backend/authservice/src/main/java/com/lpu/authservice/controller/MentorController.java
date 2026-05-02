package com.lpu.authservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import com.lpu.authservice.entity.MentorApplication;
import com.lpu.authservice.service.MentorApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/user")
@Tag(name = "User", description = "User actions (requires ROLE_USER JWT)")
public class MentorController {

    @Autowired
    private MentorApplicationService service;

    @Operation(
        summary = "Apply to become a mentor",
        description = "Submit a mentor application. Requires USER token.",
        security = @SecurityRequirement(name = "BearerAuth"),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"userId\":1,\"skillIds\":[1,2],\"experience\":3}")
            )
        ),
        responses = {
            @ApiResponse(responseCode = "201", description = "Application Submitted"),
            @ApiResponse(responseCode = "403", description = "Access denied — not USER")
        }
    )
    @PostMapping("/mentor-application")
    public ResponseEntity<String> apply(@RequestBody MentorApplication app) {
        return ResponseEntity.status(201).body(service.apply(app));
    }

    @GetMapping("/mentor-application")
    public ResponseEntity<java.util.List<MentorApplication>> myApplications(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(service.getApplicationsForUser(userId));
    }

    @PutMapping("/mentor-skills")
    public ResponseEntity<String> updateMentorSkills(
            @RequestBody java.util.List<Long> skillIds,
            HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(service.updateMentorSkills(userId, skillIds));
    }

    @DeleteMapping("/mentor-application/{id}")
    public ResponseEntity<String> revoke(@PathVariable Long id, HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        return ResponseEntity.ok(service.revokeApplication(id, userId));
    }
}
