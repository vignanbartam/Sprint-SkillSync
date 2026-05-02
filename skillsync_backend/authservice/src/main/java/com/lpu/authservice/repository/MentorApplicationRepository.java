package com.lpu.authservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import com.lpu.authservice.entity.MentorApplication;

public interface MentorApplicationRepository extends JpaRepository<MentorApplication, Long> {
    List<MentorApplication> findAllByOrderByIdDesc();
    List<MentorApplication> findByStatusOrderByIdDesc(String status);
    List<MentorApplication> findByUserIdOrderByIdDesc(Long userId);
    Optional<MentorApplication> findFirstByUserIdAndStatusOrderByIdDesc(Long userId, String status);
}
