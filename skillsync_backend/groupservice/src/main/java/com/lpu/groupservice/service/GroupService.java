package com.lpu.groupservice.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lpu.groupservice.dto.GroupRequestDTO;
import com.lpu.groupservice.dto.GroupMemberResponseDTO;
import com.lpu.groupservice.dto.GroupMessageRequestDTO;
import com.lpu.groupservice.dto.GroupMessageResponseDTO;
import com.lpu.groupservice.dto.GroupResponseDTO;
import com.lpu.groupservice.entity.Group;
import com.lpu.groupservice.entity.GroupMember;
import com.lpu.groupservice.entity.GroupMessage;
import com.lpu.groupservice.repository.GroupMemberRepository;
import com.lpu.groupservice.repository.GroupMessageRepository;
import com.lpu.groupservice.repository.GroupRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepo;
    private final GroupMemberRepository memberRepo;
    private final GroupMessageRepository messageRepo;
    private static final Long DEFAULT_ADMIN_USER_ID = 1L;

    // Create Group
    public GroupResponseDTO create(GroupRequestDTO dto, Long userId) {

        Group group = Group.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        Group saved = groupRepo.save(group);

        // creator auto join
        memberRepo.save(GroupMember.builder()
                .groupId(saved.getId())
                .userId(userId)
                .status("APPROVED")
                .joinedAt(LocalDateTime.now())
                .build());

        if (!DEFAULT_ADMIN_USER_ID.equals(userId)) {
            memberRepo.save(GroupMember.builder()
                    .groupId(saved.getId())
                    .userId(DEFAULT_ADMIN_USER_ID)
                    .status("APPROVED")
                    .joinedAt(LocalDateTime.now())
                    .build());
        }

        return map(saved);
    }

    // Get All
    public List<GroupResponseDTO> getAllGroups() {
        return groupRepo.findAll()
                .stream()
                .map(this::map)
                .toList();
    }

    public List<GroupResponseDTO> getMyGroups(Long userId) {
        return memberRepo.findByUserIdAndStatus(userId, "APPROVED")
                .stream()
                .map(member -> groupRepo.findById(member.getGroupId())
                        .orElseThrow(() -> new EntityNotFoundException("Group not found")))
                .distinct()
                .map(this::map)
                .toList();
    }

    // Join Request
    public String join(Long groupId, Long userId) {

        // Check group exists
        groupRepo.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        List<GroupMember> existingMembers = memberRepo.findAllByGroupIdAndUserId(groupId, userId);
        if (!existingMembers.isEmpty()) {
            existingMembers.forEach(member -> {
                if (!"APPROVED".equals(member.getStatus())) {
                    member.setStatus("APPROVED");
                    member.setJoinedAt(LocalDateTime.now());
                }
            });
            memberRepo.saveAll(existingMembers);
            return "Joined group";
        }

        memberRepo.save(GroupMember.builder()
                .groupId(groupId)
                .userId(userId)
                .status("APPROVED")
                .joinedAt(LocalDateTime.now())
                .build());

        return "Joined group";
    }

    public String requestJoin(Long groupId, Long userId) {
        return join(groupId, userId);
    }

    // Approve (ONLY CREATOR)
    public String approve(Long groupId, Long creatorId, Long userId) {
        return approve(groupId, creatorId, "", userId);
    }

    public String approve(Long groupId, Long actorId, String actorRole, Long userId) {

        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        ensureManager(group, actorId, actorRole);

        List<GroupMember> members = memberRepo.findAllByGroupIdAndUserId(groupId, userId);
        if (members.isEmpty()) {
            throw new EntityNotFoundException("User not found in group");
        }

        members.forEach(member -> member.setStatus("APPROVED"));
        memberRepo.saveAll(members);

        return "Approved";
    }

    public String addMember(Long groupId, Long actorId, String actorRole, Long userId) {

        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        ensureManager(group, actorId, actorRole);

        List<GroupMember> members = memberRepo.findAllByGroupIdAndUserId(groupId, userId);
        GroupMember member = members.isEmpty()
                ? GroupMember.builder()
                .groupId(groupId)
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build()
                : members.get(0);

        member.setStatus("APPROVED");
        if (member.getJoinedAt() == null) {
            member.setJoinedAt(LocalDateTime.now());
        }
        memberRepo.save(member);

        return "Member added";
    }

    // Remove Member
    public String remove(Long groupId, Long creatorId, Long userId) {
        return remove(groupId, creatorId, "", userId);
    }

    public String remove(Long groupId, Long actorId, String actorRole, Long userId) {

        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        ensureManager(group, actorId, actorRole);

        List<GroupMember> members = memberRepo.findAllByGroupIdAndUserId(groupId, userId);
        if (members.isEmpty()) {
            throw new EntityNotFoundException("User not found in group");
        }

        memberRepo.deleteAll(members);

        return "Removed";
    }

    @Transactional
    public String deleteGroup(Long groupId, Long actorId, String actorRole) {
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        ensureManager(group, actorId, actorRole);

        messageRepo.deleteByGroupId(groupId);
        memberRepo.deleteByGroupId(groupId);
        groupRepo.delete(group);

        return "Group deleted";
    }

    public List<GroupMemberResponseDTO> getMembers(Long groupId, Long actorId, String actorRole) {
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        return memberRepo.findByGroupId(groupId)
                .stream()
                .collect(Collectors.toMap(
                        GroupMember::getUserId,
                        member -> member,
                        this::preferApprovedMember,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .map(this::mapMember)
                .toList();
    }

    public List<GroupMessageResponseDTO> getMessages(Long groupId, Long userId) {
        ensureApprovedMember(groupId, userId);

        return messageRepo.findByGroupIdOrderByTimestampAsc(groupId)
                .stream()
                .map(this::mapMessage)
                .toList();
    }

    public GroupMessageResponseDTO saveMessage(Long groupId, Long senderId, GroupMessageRequestDTO dto) {
        ensureApprovedMember(groupId, senderId);

        if (dto == null || dto.getContent() == null || dto.getContent().trim().isBlank()) {
            throw new RuntimeException("Message content is required");
        }

        GroupMessage saved = messageRepo.save(GroupMessage.builder()
                .groupId(groupId)
                .senderId(senderId)
                .content(dto.getContent().trim())
                .timestamp(LocalDateTime.now())
                .build());

        return mapMessage(saved);
    }

    public void ensureApprovedMember(Long groupId, Long userId) {
        groupRepo.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found"));

        if (!memberRepo.existsByGroupIdAndUserIdAndStatus(groupId, userId, "APPROVED")) {
            throw new RuntimeException("Only approved group members can access group chat");
        }
    }

    // Mapper
    private GroupResponseDTO map(Group g) {
        return GroupResponseDTO.builder()
                .id(g.getId())
                .name(g.getName())
                .description(g.getDescription())
                .createdBy(g.getCreatedBy())
                .build();
    }

    private GroupMessageResponseDTO mapMessage(GroupMessage message) {
        return GroupMessageResponseDTO.builder()
                .id(message.getId())
                .groupId(message.getGroupId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .build();
    }

    private GroupMemberResponseDTO mapMember(GroupMember member) {
        return GroupMemberResponseDTO.builder()
                .id(member.getId())
                .groupId(member.getGroupId())
                .userId(member.getUserId())
                .displayName("User " + member.getUserId())
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    private void ensureManager(Group group, Long actorId, String actorRole) {
        boolean admin = "ROLE_ADMIN".equals(actorRole);
        if (!admin && !group.getCreatedBy().equals(actorId)) {
            throw new RuntimeException("Only group creator or admin can manage members");
        }
    }

    private GroupMember preferApprovedMember(GroupMember first, GroupMember second) {
        if ("APPROVED".equals(first.getStatus())) {
            return first;
        }
        return "APPROVED".equals(second.getStatus()) ? second : first;
    }
}
