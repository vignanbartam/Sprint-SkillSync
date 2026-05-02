package com.lpu.reviewservice.service;

import com.lpu.reviewservice.entity.Review;
import com.lpu.reviewservice.repository.ReviewRepository;
import com.lpu.reviewservice.client.SessionClient;
import com.lpu.reviewservice.exception.CustomException;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository repo;
    private final SessionClient sessionClient;

    @CacheEvict(value = "mentorReviews", key = "#review.mentorId")
    public Review addReview(Review review) {

        // check session completed
        boolean allowed;
        try {
            allowed = sessionClient.hasCompletedSession(
                    review.getUserId(),
                    review.getMentorId()
            );
        } catch (Exception e) {
            throw new CustomException("Session service unavailable");
        }

        if (!allowed) {
            throw new CustomException("Complete session first");
        }

        // prevent duplicate review
        if (repo.existsByUserIdAndMentorId(
                review.getUserId(), review.getMentorId())) {

            throw new CustomException("Already reviewed");
        }

        return repo.save(review);
    }

    @Cacheable(value = "mentorReviews", key = "#mentorId")
    public List<Review> getReviews(Long mentorId) {
        return repo.findByMentorId(mentorId);
    }
}
