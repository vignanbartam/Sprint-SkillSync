package com.lpu.authservice.service;

import org.springframework.test.context.ActiveProfiles;

import com.lpu.authservice.entity.MentorApplication;
import com.lpu.authservice.entity.User;
import com.lpu.authservice.repository.MentorApplicationRepository;
import com.lpu.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MentorApplicationServiceTest {

    @Mock private MentorApplicationRepository repo;
    @Mock private UserRepository userRepo;

    @InjectMocks
    private MentorApplicationService service;

    // ✅ Apply: success
    @Test
    void testApply_Success() {
        MentorApplication app = MentorApplication.builder()
                .userId(1L).skillIds(List.of(1L, 2L)).experience(3).build();
        when(repo.save(any())).thenReturn(app);

        String result = service.apply(app);

        assertEquals("Application Submitted", result);
        assertEquals("PENDING", app.getStatus());
        verify(repo, times(1)).save(app);
    }

    // ✅ Approve: success
    @Test
    void testApprove_Success() {
        MentorApplication app = MentorApplication.builder()
                .id(1L).userId(2L).status("PENDING").build();
        User user = User.builder()
                .id(2L)
                .email("mentor@gmail.com")
                .password("pass")
                .role("ROLE_USER")
                .mentorApproved(false)
                .build();

        when(repo.findById(1L)).thenReturn(Optional.of(app));
        when(userRepo.findById(2L)).thenReturn(Optional.of(user));
        when(userRepo.save(any())).thenReturn(user);

        String result = service.approve(1L);

        assertEquals("Mentor Approved", result);
        assertEquals("ROLE_MENTOR", user.getRole());
        assertTrue(user.isMentorApproved());
        assertEquals("APPROVED", app.getStatus());
    }

    // ❌ Approve: admin cannot become mentor
    @Test
    void testApprove_AdminCannotBecomeMentor() {
        MentorApplication app = MentorApplication.builder()
                .id(1L).userId(2L).status("PENDING").build();
        User admin = User.builder()
                .id(2L)
                .email("admin@gmail.com")
                .password("pass")
                .role("ROLE_ADMIN")
                .mentorApproved(false)
                .build();

        when(repo.findById(1L)).thenReturn(Optional.of(app));
        when(userRepo.findById(2L)).thenReturn(Optional.of(admin));

        assertThrows(RuntimeException.class, () -> service.approve(1L));
        verify(userRepo, never()).save(any());
    }

    // ❌ Approve: application not found
    @Test
    void testApprove_ApplicationNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> service.approve(99L));
    }
}
