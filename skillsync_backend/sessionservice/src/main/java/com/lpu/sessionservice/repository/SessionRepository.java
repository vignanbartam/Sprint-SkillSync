package com.lpu.sessionservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.lpu.sessionservice.entity.Session;

public interface SessionRepository extends JpaRepository<Session, Long> {
	boolean existsByUserIdAndMentorIdAndStatus(
		    Long userId, Long mentorId, String status);
    List<Session> findByMentorIdOrderByIdDesc(Long mentorId);
    List<Session> findByUserIdOrderByIdDesc(Long userId);
    List<Session> findAllByOrderByIdDesc();
}
