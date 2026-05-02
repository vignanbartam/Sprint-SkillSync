package com.lpu.skillservice.repository;


import com.lpu.skillservice.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {
	boolean existsByName(String name);
}
