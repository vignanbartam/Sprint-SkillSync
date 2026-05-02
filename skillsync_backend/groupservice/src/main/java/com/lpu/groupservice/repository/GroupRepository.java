package com.lpu.groupservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lpu.groupservice.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {
}
