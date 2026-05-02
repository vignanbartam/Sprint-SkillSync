package com.lpu.reviewservice.service;

import org.springframework.test.context.ActiveProfiles;

import com.lpu.reviewservice.client.SessionClient;
import com.lpu.reviewservice.entity.Review;
import com.lpu.reviewservice.exception.CustomException;
import com.lpu.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository repo;
    @Mock private SessionClient sessionClient;

    @InjectMocks
    private ReviewService service;

    private Review buildReview() {
        Review r = new Review();
        r.setUserId(1L);
        r.setMentorId(2L);
        r.setRating(5);
        r.setComment("Great mentor!");
        return r;
    }

    // ✅ Add review: success
    @Test
    void testAddReview_Success() {
        Review review = buildReview();
        when(sessionClient.hasCompletedSession(1L, 2L)).thenReturn(true);
        when(repo.existsByUserIdAndMentorId(1L, 2L)).thenReturn(false);
        when(repo.save(any())).thenReturn(review);

        Review result = service.addReview(review);

        assertEquals(5, result.getRating());
        verify(repo, times(1)).save(any());
    }

    // ❌ Add review: session not completed
    @Test
    void testAddReview_SessionNotCompleted() {
        Review review = buildReview();
        when(sessionClient.hasCompletedSession(1L, 2L)).thenReturn(false);

        assertThrows(CustomException.class, () -> service.addReview(review));
        verify(repo, never()).save(any());
    }

    // ❌ Add review: already reviewed
    @Test
    void testAddReview_AlreadyReviewed() {
        Review review = buildReview();
        when(sessionClient.hasCompletedSession(1L, 2L)).thenReturn(true);
        when(repo.existsByUserIdAndMentorId(1L, 2L)).thenReturn(true);

        assertThrows(CustomException.class, () -> service.addReview(review));
    }

    // ❌ Add review: session service unavailable
    @Test
    void testAddReview_SessionServiceDown() {
        Review review = buildReview();
        when(sessionClient.hasCompletedSession(1L, 2L)).thenThrow(new RuntimeException("Service down"));

        assertThrows(CustomException.class, () -> service.addReview(review));
    }

    // ✅ Get reviews: success
    @Test
    void testGetReviews_Success() {
        Review r1 = buildReview();
        when(repo.findByMentorId(2L)).thenReturn(List.of(r1));

        List<Review> result = service.getReviews(2L);

        assertEquals(1, result.size());
    }

    // ✅ Get reviews: no reviews found
    @Test
    void testGetReviews_EmptyList() {
        when(repo.findByMentorId(99L)).thenReturn(List.of());

        List<Review> result = service.getReviews(99L);

        assertTrue(result.isEmpty());
    }
}
