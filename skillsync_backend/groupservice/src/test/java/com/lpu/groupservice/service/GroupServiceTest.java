package com.lpu.groupservice.service;

import org.springframework.test.context.ActiveProfiles;

import com.lpu.groupservice.dto.GroupRequestDTO;
import com.lpu.groupservice.dto.GroupResponseDTO;
import com.lpu.groupservice.entity.Group;
import com.lpu.groupservice.entity.GroupMember;
import com.lpu.groupservice.repository.GroupMemberRepository;
import com.lpu.groupservice.repository.GroupRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock private GroupRepository groupRepo;
    @Mock private GroupMemberRepository memberRepo;

    @InjectMocks
    private GroupService service;

    private Group buildGroup(Long id, Long creatorId) {
        return Group.builder()
                .id(id).name("Test Group").description("Desc")
                .createdBy(creatorId).createdAt(LocalDateTime.now()).build();
    }

    private GroupRequestDTO buildDto() {
        GroupRequestDTO dto = new GroupRequestDTO();
        dto.setName("Test Group");
        dto.setDescription("Desc");
        return dto;
    }

    // ✅ Create group: success
    @Test
    void testCreate_Success() {
        Group saved = buildGroup(1L, 10L);
        when(groupRepo.save(any())).thenReturn(saved);
        when(memberRepo.save(any())).thenReturn(new GroupMember());

        GroupResponseDTO result = service.create(buildDto(), 10L);

        assertEquals("Test Group", result.getName());
        assertEquals(10L, result.getCreatedBy());
        verify(memberRepo, times(2)).save(any()); // creator and admin auto-joined
    }

    // ✅ Get all groups: success
    @Test
    void testGetAllGroups_Success() {
        when(groupRepo.findAll()).thenReturn(List.of(buildGroup(1L, 10L), buildGroup(2L, 20L)));

        List<GroupResponseDTO> result = service.getAllGroups();

        assertEquals(2, result.size());
    }

    // ✅ Get all groups: empty list
    @Test
    void testGetAllGroups_Empty() {
        when(groupRepo.findAll()).thenReturn(List.of());

        List<GroupResponseDTO> result = service.getAllGroups();

        assertTrue(result.isEmpty());
    }

    // ✅ Request join: success
    @Test
    void testRequestJoin_Success() {
        when(groupRepo.findById(1L)).thenReturn(Optional.of(buildGroup(1L, 10L)));
        when(memberRepo.findAllByGroupIdAndUserId(1L, 5L)).thenReturn(List.of());
        when(memberRepo.save(any())).thenReturn(new GroupMember());

        String result = service.requestJoin(1L, 5L);

        assertEquals("Joined group", result);
    }

    // ❌ Request join: group not found
    @Test
    void testRequestJoin_GroupNotFound() {
        when(groupRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.requestJoin(99L, 5L));
    }

    // ✅ Approve member: success
    @Test
    void testApprove_Success() {
        Group group = buildGroup(1L, 10L);
        GroupMember member = GroupMember.builder()
                .groupId(1L).userId(5L).status("PENDING").build();

        when(groupRepo.findById(1L)).thenReturn(Optional.of(group));
        when(memberRepo.findAllByGroupIdAndUserId(1L, 5L)).thenReturn(List.of(member));
        when(memberRepo.saveAll(any())).thenReturn(List.of(member));

        String result = service.approve(1L, 10L, 5L);

        assertEquals("Approved", result);
        assertEquals("APPROVED", member.getStatus());
    }

    // ❌ Approve member: not the creator
    @Test
    void testApprove_NotCreator() {
        Group group = buildGroup(1L, 10L);
        when(groupRepo.findById(1L)).thenReturn(Optional.of(group));

        assertThrows(RuntimeException.class, () -> service.approve(1L, 99L, 5L));
    }

    // ❌ Approve member: group not found
    @Test
    void testApprove_GroupNotFound() {
        when(groupRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.approve(99L, 10L, 5L));
    }

    // ✅ Remove member: success
    @Test
    void testRemove_Success() {
        Group group = buildGroup(1L, 10L);
        GroupMember member = GroupMember.builder()
                .groupId(1L).userId(5L).status("APPROVED").build();

        when(groupRepo.findById(1L)).thenReturn(Optional.of(group));
        when(memberRepo.findAllByGroupIdAndUserId(1L, 5L)).thenReturn(List.of(member));

        String result = service.remove(1L, 10L, 5L);

        assertEquals("Removed", result);
        verify(memberRepo, times(1)).deleteAll(List.of(member));
    }

    // ❌ Remove member: not the creator
    @Test
    void testRemove_NotCreator() {
        Group group = buildGroup(1L, 10L);
        when(groupRepo.findById(1L)).thenReturn(Optional.of(group));

        assertThrows(RuntimeException.class, () -> service.remove(1L, 99L, 5L));
    }

    // ❌ Remove member: user not in group
    @Test
    void testRemove_MemberNotFound() {
        Group group = buildGroup(1L, 10L);
        when(groupRepo.findById(1L)).thenReturn(Optional.of(group));
        when(memberRepo.findAllByGroupIdAndUserId(1L, 99L)).thenReturn(List.of());

        assertThrows(EntityNotFoundException.class, () -> service.remove(1L, 10L, 99L));
    }
}
