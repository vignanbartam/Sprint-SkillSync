package com.lpu.groupservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lpu.groupservice.entity.GroupMember;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroupId(Long groupId);

    void deleteByGroupId(Long groupId);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    List<GroupMember> findAllByGroupIdAndUserId(Long groupId, Long userId);

    List<GroupMember> findByUserIdAndStatus(Long userId, String status);

    boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, String status);
}
