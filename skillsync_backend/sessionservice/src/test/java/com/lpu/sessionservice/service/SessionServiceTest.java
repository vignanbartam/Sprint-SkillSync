package com.lpu.sessionservice.service;

import com.lpu.sessionservice.client.AuthClient;
import com.lpu.sessionservice.dto.SessionRequestDTO;
import com.lpu.sessionservice.dto.SessionResponseDTO;
import com.lpu.sessionservice.dto.SessionUpdateRequestDTO;
import com.lpu.sessionservice.dto.UserDTO;
import com.lpu.sessionservice.entity.Session;
import com.lpu.sessionservice.exception.CustomException;
import com.lpu.sessionservice.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock private SessionRepository repo;
    @Mock private AuthClient authClient;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private SessionService service;

    // ✅ Book session: success
    @Test
    void testBook_Success() {
        SessionRequestDTO dto = new SessionRequestDTO(1L, 2L);
        Session saved = Session.builder().id(10L).userId(1L).mentorId(2L).status("PENDING").build();
        when(repo.save(any())).thenReturn(saved);

        SessionResponseDTO result = service.book(dto, 1L);

        assertEquals("PENDING", result.getStatus());
        assertEquals(1L, result.getUserId());
        verify(repo, times(1)).save(any());
    }

    // ❌ Book session: missing userId
    @Test
    void testBook_MissingUserId() {
        SessionRequestDTO dto = new SessionRequestDTO(null, 2L);
        assertThrows(CustomException.class, () -> service.book(dto, null));
    }

    // ❌ Book session: missing mentorId
    @Test
    void testBook_MissingMentorId() {
        SessionRequestDTO dto = new SessionRequestDTO(1L, null);
        assertThrows(CustomException.class, () -> service.book(dto, 1L));
    }

    // ✅ Update status: ACCEPTED
    @Test
    void testUpdateStatus_Accepted() {
        Session session = Session.builder().id(1L).userId(1L).mentorId(2L).status("PENDING").build();
        when(repo.findById(1L)).thenReturn(Optional.of(session));
        when(repo.save(any())).thenReturn(session);
        UserDTO user1 = new UserDTO(); user1.setId(1L); user1.setEmail("user@gmail.com");
        when(authClient.getUserById(1L)).thenReturn(user1);

        SessionUpdateRequestDTO dto = new SessionUpdateRequestDTO();
        dto.setTimeSlot("2026-05-02T10:00");
        dto.setMeetingUrl("https://meet.google.com/test");

        SessionResponseDTO result = service.updateStatus(1L, "ACCEPTED", 2L, dto);

        assertEquals("ACCEPTED", result.getStatus());
    }

    // ✅ Update status: COMPLETED
    @Test
    void testUpdateStatus_Completed() {
        Session session = Session.builder().id(1L).userId(1L).mentorId(2L).status("ACCEPTED").build();
        when(repo.findById(1L)).thenReturn(Optional.of(session));
        when(repo.save(any())).thenReturn(session);
        UserDTO user2 = new UserDTO(); user2.setId(1L); user2.setEmail("user@gmail.com");
        when(authClient.getUserById(1L)).thenReturn(user2);

        SessionResponseDTO result = service.updateStatus(1L, "COMPLETED", 2L);

        assertEquals("COMPLETED", result.getStatus());
    }

    // ❌ Update status: session not found
    @Test
    void testUpdateStatus_SessionNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(CustomException.class, () -> service.updateStatus(99L, "ACCEPTED", 2L));
    }

    // ❌ Update status: auth service unavailable
    @Test
    void testUpdateStatus_AuthServiceDown() {
        Session session = Session.builder().id(1L).userId(1L).mentorId(2L).status("PENDING").build();
        when(repo.findById(1L)).thenReturn(Optional.of(session));
        when(repo.save(any())).thenReturn(session);
        when(authClient.getUserById(1L)).thenThrow(new RuntimeException("Auth service down"));

        SessionUpdateRequestDTO dto = new SessionUpdateRequestDTO();
        dto.setTimeSlot("2026-05-02T10:00");
        dto.setMeetingUrl("https://meet.google.com/test");

        assertThrows(CustomException.class, () -> service.updateStatus(1L, "ACCEPTED", 2L, dto));
    }

    @Test
    void testUpdateStatus_WrongMentorDenied() {
        Session session = Session.builder().id(1L).userId(1L).mentorId(2L).status("PENDING").build();
        when(repo.findById(1L)).thenReturn(Optional.of(session));

        CustomException ex = assertThrows(CustomException.class, () -> service.updateStatus(1L, "ACCEPTED", 3L));
        assertEquals("Only the assigned mentor can manage this session", ex.getMessage());
    }
}
