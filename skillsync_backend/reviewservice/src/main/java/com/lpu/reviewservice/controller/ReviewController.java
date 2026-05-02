package com.lpu.reviewservice.controller;

import com.lpu.reviewservice.entity.Review;
import com.lpu.reviewservice.service.ReviewService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "Submit and read mentor reviews")
public class ReviewController {

    private final ReviewService service;

    @Operation(
        summary = "Add a review for a mentor",
        description = "User must have a COMPLETED session with the mentor before reviewing.",
        security = @SecurityRequirement(name = "BearerAuth"),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"userId\":1,\"mentorId\":2,\"rating\":5,\"comment\":\"Excellent mentor!\"}")
            )
        ),
        responses = {
            @ApiResponse(responseCode = "201", description = "Review added"),
            @ApiResponse(responseCode = "400", description = "No completed session / already reviewed"),
            @ApiResponse(responseCode = "403", description = "Access denied — not USER")
        }
    )
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Review> addReview(@RequestBody Review review) {
        return ResponseEntity.status(201).body(service.addReview(review));
    }

    @Operation(
        summary = "Get all reviews for a mentor",
        description = "Public endpoint — no token required.",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of reviews"),
            @ApiResponse(responseCode = "404", description = "No reviews found")
        }
    )
    @GetMapping("/mentor/{mentorId}")
    public ResponseEntity<List<Review>> getReviews(
            @Parameter(description = "Mentor's user ID", example = "2") @PathVariable Long mentorId) {
        return ResponseEntity.ok(service.getReviews(mentorId));
    }
}
