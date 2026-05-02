package com.lpu.authservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.lpu.authservice.entity.MentorApplication;
import com.lpu.authservice.dto.DeleteUserRequestDTO;
import com.lpu.authservice.dto.UserDTO;
import com.lpu.authservice.service.AuthService;
import com.lpu.authservice.service.MentorApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Admin-only operations (requires ROLE_ADMIN JWT)")
public class AdminController {

    @Autowired
    private MentorApplicationService service;

    @Autowired
    private AuthService authService;

    @Operation(
        summary = "Get all mentor applications",
        description = "Returns all mentor applications for admin review. Requires ADMIN token.",
        security = @SecurityRequirement(name = "BearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Applications fetched"),
            @ApiResponse(responseCode = "403", description = "Access denied — not ADMIN")
        }
    )
    @GetMapping("/mentor-applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MentorApplication>> getApplications() {
        return ResponseEntity.ok(service.getAllApplications());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getUsers() {
        return ResponseEntity.ok(authService.getAllUsersForAdmin());
    }

    @Operation(
        summary = "Approve a mentor application",
        description = "Promotes a user to ROLE_MENTOR. Requires ADMIN token.",
        security = @SecurityRequirement(name = "BearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Mentor Approved"),
            @ApiResponse(responseCode = "403", description = "Access denied — not ADMIN"),
            @ApiResponse(responseCode = "404", description = "Application not found")
        }
    )
    @PutMapping("/mentors/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> approve(
            @Parameter(description = "MentorApplication ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @Operation(
        summary = "Reject a mentor application",
        description = "Rejects a pending mentor application. Requires ADMIN token.",
        security = @SecurityRequirement(name = "BearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Mentor Application Rejected"),
            @ApiResponse(responseCode = "403", description = "Access denied — not ADMIN"),
            @ApiResponse(responseCode = "404", description = "Application not found")
        }
    )
    @PutMapping("/mentors/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> reject(
            @Parameter(description = "MentorApplication ID", example = "1") @PathVariable Long id,
            @RequestBody(required = false) DeleteUserRequestDTO request) {
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(service.reject(id, reason));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(
            @Parameter(description = "User ID", example = "5") @PathVariable Long id,
            @RequestBody DeleteUserRequestDTO request) {
        return ResponseEntity.ok(service.deleteUser(id, request == null ? null : request.getReason()));
    }

    @PutMapping("/users/{id}/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateUserRole(
            @Parameter(description = "User ID", example = "5") @PathVariable Long id,
            @PathVariable String role) {
        return ResponseEntity.ok(service.updateUserRole(id, role));
    }
}
