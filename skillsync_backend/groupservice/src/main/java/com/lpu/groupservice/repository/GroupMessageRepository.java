package com.lpu.groupservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lpu.groupservice.entity.GroupMessage;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    List<GroupMessage> findByGroupIdOrderByTimestampAsc(Long groupId);

    void deleteByGroupId(Long groupId);
}
