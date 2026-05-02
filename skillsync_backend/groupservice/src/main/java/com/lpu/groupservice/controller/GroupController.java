package com.lpu.groupservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.lpu.groupservice.config.UserContext;
import com.lpu.groupservice.dto.GroupMemberResponseDTO;
import com.lpu.groupservice.dto.GroupMessageResponseDTO;
import com.lpu.groupservice.dto.GroupRequestDTO;
import com.lpu.groupservice.dto.GroupResponseDTO;
import com.lpu.groupservice.service.GroupService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Create and manage study/skill groups")
public class GroupController {

    private final GroupService service;
    private final UserContext userContext;

    @Operation(
        summary = "Create a new group",
        description = "Creates a group. The creator and admin are auto-approved as members. Requires JWT (X-User-Id forwarded by gateway).",
        security = @SecurityRequirement(name = "BearerAuth"),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"name\":\"Java Learners\",\"description\":\"Group for Java learners\"}")
            )
        ),
        responses = {
            @ApiResponse(responseCode = "201", description = "Group created"),
            @ApiResponse(responseCode = "401", description = "No valid token / X-User-Id missing")
        }
    )
    @PostMapping
    public ResponseEntity<GroupResponseDTO> create(@RequestBody GroupRequestDTO dto,
                                                   HttpServletRequest request) {
        Long userId = userContext.getUserId(request);
        return ResponseEntity.status(201).body(service.create(dto, userId));
    }

    @Operation(
        summary = "List all groups",
        description = "Public endpoint — no token required.",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of groups")
        }
    )
    @GetMapping
    public ResponseEntity<List<GroupResponseDTO>> getAllGroups() {
        return ResponseEntity.ok(service.getAllGroups());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<GroupResponseDTO>> getMyGroups(HttpServletRequest request) {
        Long userId = userContext.getUserId(request);
        return ResponseEntity.ok(service.getMyGroups(userId));
    }

    @Operation(
        summary = "Request to join a group",
        description = "Joins the authenticated user immediately. Requires JWT.",
        security = @SecurityRequirement(name = "BearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Request sent"),
            @ApiResponse(responseCode = "404", description = "Group not found")
        }
    )
    @PostMapping("/{id}/join")
    public ResponseEntity<String> join(
            @Parameter(description = "Group ID", example = "1") @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = userContext.getUserId(request);
        return ResponseEntity.ok(service.join(id, userId));
    }

    @Operation(
        summary = "Approve a member's join request",
        description = "Only the group creator can approve members. Requires JWT.",
        security = @SecurityRequirement(name = "BearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Approved"),
            @ApiResponse(responseCode = "403", description = "Not the group creator"),
            @ApiResponse(responseCode = "404", description = "Group or member not found")
        }
    )
    @PutMapping("/{id}/approve/{userId}")
    public ResponseEntity<String> approve(
            @Parameter(description = "Group ID", example = "1") @PathVariable Long id,
            @Parameter(description = "User ID to approve", example = "5") @PathVariable Long userId,
            HttpServletRequest request) {
        Long actorId = userContext.getUserId(request);
        String actorRole = userContext.getRole(request);
        return ResponseEntity.ok(service.approve(id, actorId, actorRole, userId));
    }

    @PostMapping("/{id}/members/{userId}")
    public ResponseEntity<String> addMember(
            @Parameter(description = "Group ID", example = "1") @PathVariable Long id,
            @Parameter(description = "User ID to add", example = "5") @PathVariable Long userId,
            HttpServletRequest request) {
        Long actorId = userContext.getUserId(request);
        String actorRole = userContext.getRole(request);
        return ResponseEntity.ok(service.addMember(id, actorId, actorRole, userId));
    }

    @Operation(
        summary = "Remove a member from a group",
        description = "Only the group creator can remove members. Requires JWT.",
        security = @SecurityRequirement(name = "BearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Removed"),
            @ApiResponse(responseCode = "403", description = "Not the group creator"),
            @ApiResponse(responseCode = "404", description = "Group or member not found")
        }
    )
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<String> remove(
            @Parameter(description = "Group ID", example = "1") @PathVariable Long id,
            @Parameter(description = "User ID to remove", example = "5") @PathVariable Long userId,
            HttpServletRequest request) {
        Long actorId = userContext.getUserId(request);
        String actorRole = userContext.getRole(request);
        return ResponseEntity.ok(service.remove(id, actorId, actorRole, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteGroup(
            @Parameter(description = "Group ID", example = "1") @PathVariable Long id,
            HttpServletRequest request) {
        Long actorId = userContext.getUserId(request);
        String actorRole = userContext.getRole(request);
        return ResponseEntity.ok(service.deleteGroup(id, actorId, actorRole));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<GroupMemberResponseDTO>> getMembers(
            @Parameter(description = "Group ID", example = "1") @PathVariable Long id,
            HttpServletRequest request) {
        Long actorId = userContext.getUserId(request);
        String actorRole = userContext.getRole(request);
        return ResponseEntity.ok(service.getMembers(id, actorId, actorRole));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<GroupMessageResponseDTO>> getMessages(
            @Parameter(description = "Group ID", example = "1") @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = userContext.getUserId(request);
        return ResponseEntity.ok(service.getMessages(id, userId));
    }
}
