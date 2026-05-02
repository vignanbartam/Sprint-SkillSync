package com.lpu.sessionservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.lpu.sessionservice.dto.SessionRequestDTO;
import com.lpu.sessionservice.dto.SessionResponseDTO;
import com.lpu.sessionservice.dto.SessionUpdateRequestDTO;
import com.lpu.sessionservice.repository.SessionRepository;
import com.lpu.sessionservice.service.SessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequestMapping("/session")
@Tag(name = "Session", description = "Book and manage mentoring sessions")
public class SessionController {

    @Autowired
    private SessionService service;

    @Autowired
    private SessionRepository repo;

    @Operation(
        summary = "Book a session",
        description = "A user books a session with a mentor. Status starts as PENDING.",
        security = @SecurityRequirement(name = "BearerAuth"),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"userId\":1,\"mentorId\":2}")
            )
        ),
        responses = {
            @ApiResponse(responseCode = "201", description = "Session booked"),
            @ApiResponse(responseCode = "400", description = "Missing userId or mentorId")
        }
    )
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SessionResponseDTO> book(@RequestBody SessionRequestDTO dto) {
        return ResponseEntity.status(201).body(service.book(dto, service.getAuthenticatedUserId()));
    }

    @Operation(
        summary = "Update session status",
        description = "Mentor updates session status. Valid values: ACCEPTED, REJECTED.",
        security = @SecurityRequirement(name = "BearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "404", description = "Session not found")
        }
    )
    @PutMapping("/{id}/{status}")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<SessionResponseDTO> update(
            @Parameter(description = "Session ID", example = "1") @PathVariable Long id,
            @Parameter(description = "New status: ACCEPTED or REJECTED", example = "ACCEPTED") @PathVariable String status,
            @RequestBody(required = false) SessionUpdateRequestDTO dto) {
        return ResponseEntity.ok(service.updateStatus(id, status, service.getAuthenticatedUserId(), dto));
    }

    @Operation(
        summary = "Check if session completed (internal)",
        description = "Used internally by reviewservice via Feign. Returns true if a COMPLETED session exists.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Returns true or false")
        }
    )
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkCompleted(
            @Parameter(description = "User ID", example = "1") @RequestParam Long userId,
            @Parameter(description = "Mentor ID", example = "2") @RequestParam Long mentorId) {
        boolean result = repo.existsByUserIdAndMentorIdAndStatus(userId, mentorId, "COMPLETED");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/mentor")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<List<SessionResponseDTO>> getMentorSessions() {
        return ResponseEntity.ok(service.getMentorSessions(service.getAuthenticatedUserId()));
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<SessionResponseDTO>> getUserSessions() {
        return ResponseEntity.ok(service.getUserSessions(service.getAuthenticatedUserId()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SessionResponseDTO>> getAllSessionsForAdmin() {
        return ResponseEntity.ok(service.getAllSessionsForAdmin());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','MENTOR')")
    public ResponseEntity<Void> deleteSession(
            @Parameter(description = "Session ID", example = "1") @PathVariable Long id) {
        service.deleteSession(id, service.getAuthenticatedUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Mark session as completed",
        description = "Marks a session as COMPLETED and notifies the user.",
        security = @SecurityRequirement(name = "BearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Session completed"),
            @ApiResponse(responseCode = "404", description = "Session not found")
        }
    )
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<SessionResponseDTO> completeSession(
            @Parameter(description = "Session ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(service.updateStatus(id, "COMPLETED", service.getAuthenticatedUserId()));
    }
}
