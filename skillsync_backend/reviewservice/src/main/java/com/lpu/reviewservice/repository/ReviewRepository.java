package com.lpu.reviewservice.repository;

import com.lpu.reviewservice.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByMentorId(Long mentorId);

    boolean existsByUserIdAndMentorId(Long userId, Long mentorId); // prevent duplicates
}