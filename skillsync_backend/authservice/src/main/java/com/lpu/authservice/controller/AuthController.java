package com.lpu.authservice.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.lpu.authservice.dto.MentorSummaryDTO;
import com.lpu.authservice.dto.BinaryFileDTO;
import com.lpu.authservice.dto.ProfileUpdateDTO;
import com.lpu.authservice.dto.UserDTO;
import com.lpu.authservice.dto.VerifyRegistrationDTO;
import com.lpu.authservice.entity.User;
import com.lpu.authservice.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Register, login, and fetch user info")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(
        summary = "Register a new user",
        description = "Creates a new account with ROLE_USER. Email must be unique.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"email\":\"john@gmail.com\",\"password\":\"pass123\"}")
            )
        ),
        responses = {
            @ApiResponse(responseCode = "201", description = "Registered successfully"),
            @ApiResponse(responseCode = "400", description = "Email already exists")
        }
    )
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        return ResponseEntity.status(201).body(authService.register(user));
    }

    @PostMapping("/register/verify")
    public ResponseEntity<String> verifyRegistration(@RequestBody VerifyRegistrationDTO dto) {
        return ResponseEntity.status(201).body(authService.verifyRegistration(dto));
    }

    @Operation(
        summary = "Login and get JWT token",
        description = "Returns a JWT Bearer token. Use it in the Authorize button above.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"email\":\"admin@gmail.com\",\"password\":\"admin123\"}")
            )
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "JWT token returned"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials")
        }
    )
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        String token = authService.login(user.getEmail(), user.getPassword());
        return ResponseEntity.ok(token);
    }

    @Operation(
        summary = "Get user by ID",
        description = "Returns user email and ID.",
        security = @SecurityRequirement(name = "BearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
        }
    )
    @GetMapping("/user/{id}")
    public ResponseEntity<UserDTO> getUser(
            @Parameter(description = "User ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    @Operation(
        summary = "Get current profile",
        description = "Returns the authenticated user's full profile.",
        security = @SecurityRequirement(name = "BearerAuth")
    )
    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfile(Principal principal) {
        return ResponseEntity.ok(authService.getCurrentProfile(principal.getName()));
    }

    @Operation(
        summary = "Update current profile",
        description = "Updates editable profile fields for the authenticated user.",
        security = @SecurityRequirement(name = "BearerAuth")
    )
    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateProfile(@RequestBody ProfileUpdateDTO dto, Principal principal) {
        return ResponseEntity.ok(authService.updateProfile(principal.getName(), dto));
    }

    @PutMapping("/profile/role/{role}")
    public ResponseEntity<UserDTO> updateOwnRole(@PathVariable String role, Principal principal) {
        return ResponseEntity.ok(authService.updateOwnRole(principal.getName(), role));
    }

    @Operation(
        summary = "Upload profile picture",
        description = "Uploads a profile picture for the authenticated user.",
        security = @SecurityRequirement(name = "BearerAuth")
    )
    @PostMapping(value = "/profile/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDTO> uploadProfilePicture(
            @RequestPart("file") MultipartFile file,
            Principal principal) {
        return ResponseEntity.ok(authService.uploadProfilePicture(principal.getName(), file));
    }

    @Operation(
        summary = "Upload mentor biodata",
        description = "Uploads a biodata document for the authenticated mentor.",
        security = @SecurityRequirement(name = "BearerAuth")
    )
    @PostMapping(value = "/profile/biodata", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDTO> uploadBiodata(
            @RequestPart("file") MultipartFile file,
            Principal principal) {
        return ResponseEntity.ok(authService.uploadBiodata(principal.getName(), file));
    }

    @Operation(
        summary = "List mentors",
        description = "Returns public mentor profiles for session booking."
    )
    @GetMapping("/mentors")
    public ResponseEntity<List<MentorSummaryDTO>> getMentors() {
        return ResponseEntity.ok(authService.getAllMentors());
    }

    @Operation(
        summary = "Internal user lookup",
        description = "Internal endpoint used by other services to retrieve full profile details."
    )
    @GetMapping("/internal/user/{id}")
    public ResponseEntity<UserDTO> getInternalUser(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    @GetMapping("/profile-picture/{id}")
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable Long id) {
        return buildBinaryResponse(authService.getProfilePicture(id));
    }

    @GetMapping("/biodata/{id}")
    public ResponseEntity<byte[]> getBiodata(@PathVariable Long id) {
        return buildBinaryResponse(authService.getBiodata(id));
    }

    private ResponseEntity<byte[]> buildBinaryResponse(BinaryFileDTO file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (file.getFileName() == null ? "file" : file.getFileName()) + "\"")
                .contentType(MediaType.parseMediaType(file.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.getContentType()))
                .body(file.getData());
    }
}
