package com.lpu.sessionservice.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lpu.sessionservice.client.AuthClient;
import com.lpu.sessionservice.dto.NotificationEvent;
import com.lpu.sessionservice.dto.SessionRequestDTO;
import com.lpu.sessionservice.dto.SessionResponseDTO;
import com.lpu.sessionservice.dto.SessionUpdateRequestDTO;
import com.lpu.sessionservice.dto.UserDTO;
import com.lpu.sessionservice.entity.Session;
import com.lpu.sessionservice.repository.SessionRepository;
import com.lpu.sessionservice.config.RabbitConfig;
import com.lpu.sessionservice.exception.CustomException;

@Service
public class SessionService {

    @Autowired
    private SessionRepository repo;

    @Autowired
    private AuthClient authClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // BOOK SESSION
    public SessionResponseDTO book(SessionRequestDTO dto, Long requesterUserId) {

        if (requesterUserId == null || dto.getMentorId() == null) {
            throw new CustomException("Authenticated user and mentorId are required");
        }

        Session session = Session.builder()
                .userId(requesterUserId)
                .mentorId(dto.getMentorId())
                .status("PENDING")
                .durationMinutes(dto.getDurationMinutes())
                .sessionPrice(dto.getSessionPrice())
                .build();

        return map(repo.save(session), null);
    }

    // UPDATE STATUS
    public SessionResponseDTO updateStatus(Long id, String status, Long actingMentorId) {
        return updateStatus(id, status, actingMentorId, null);
    }

    public SessionResponseDTO updateStatus(Long id, String status, Long actingMentorId, SessionUpdateRequestDTO dto) {

        Session session = repo.findById(id)
                .orElseThrow(() -> new CustomException("Session not found"));

        assertMentorOwnsSession(session, actingMentorId);

        if (!status.equals("ACCEPTED") && !status.equals("REJECTED") && !status.equals("COMPLETED")) {
            throw new CustomException("Invalid session status");
        }

        session.setStatus(status);
        if ("ACCEPTED".equals(status)) {
            if (dto == null || dto.getTimeSlot() == null || dto.getTimeSlot().isBlank()
                    || dto.getMeetingUrl() == null || dto.getMeetingUrl().isBlank()) {
                throw new CustomException("Time slot and meeting URL are required when accepting a session");
            }
            session.setTimeSlot(dto.getTimeSlot().trim());
            session.setMeetingUrl(dto.getMeetingUrl().trim());
        }
        Session saved = repo.save(session);

        // CALL AUTH SERVICE SAFELY
        UserDTO user;
        try {
            user = authClient.getUserById(session.getUserId());
        } catch (Exception e) {
            throw new CustomException("Auth service unavailable");
        }

        // SEND NOTIFICATION (DON’T BREAK FLOW)
        try {

            if (status.equals("ACCEPTED")) {
                UserDTO mentor = fetchUser(session.getMentorId());

                NotificationEvent event = NotificationEvent.builder()
                        .userId(session.getUserId())
                        .email(user.getEmail())
                        .subject("Your SkillSync session is confirmed")
                        .message(buildAcceptedSessionEmail(user, mentor, session))
                        .type("SESSION_ACCEPTED")
                        .timeSlot(session.getTimeSlot())
                        .meetingUrl(session.getMeetingUrl())
                        .build();

                rabbitTemplate.convertAndSend(
                        RabbitConfig.EXCHANGE,
                        RabbitConfig.ROUTING_KEY,
                        event
                );
            }

            if (status.equals("REJECTED")) {
                UserDTO mentor = fetchUser(session.getMentorId());

                NotificationEvent event = NotificationEvent.builder()
                        .userId(session.getUserId())
                        .email(user.getEmail())
                        .subject("Your SkillSync session request was rejected")
                        .message(buildRejectedSessionEmail(user, mentor, session))
                        .type("SESSION_REJECTED")
                        .build();

                rabbitTemplate.convertAndSend(
                        RabbitConfig.EXCHANGE,
                        RabbitConfig.ROUTING_KEY,
                        event
                );
            }

            if (status.equals("COMPLETED")) {

                NotificationEvent event = NotificationEvent.builder()
                        .userId(session.getUserId())
                        .email(user.getEmail())
                        .subject("Your SkillSync session is complete")
                        .message("Your session is completed. Please give rating and review.")
                        .type("SESSION_COMPLETED")
                        .build();

                rabbitTemplate.convertAndSend(
                        RabbitConfig.EXCHANGE,
                        RabbitConfig.ROUTING_KEY,
                        event
                );
            }

        } catch (Exception e) {
            
            System.out.println("Notification failed: " + e.getMessage());
        }

        return map(saved, user);
    }

    public List<SessionResponseDTO> getMentorSessions(Long mentorId) {
        if (mentorId == null) {
            throw new CustomException("Authenticated mentor is required");
        }

        return repo.findByMentorIdOrderByIdDesc(mentorId).stream()
                .map(session -> {
                    UserDTO user = authClient.getUserById(session.getUserId());
                    return map(session, user);
                })
                .toList();
    }

    public List<SessionResponseDTO> getUserSessions(Long userId) {
        if (userId == null) {
            throw new CustomException("Authenticated user is required");
        }

        return repo.findByUserIdOrderByIdDesc(userId).stream()
                .map(session -> map(session, null, fetchUser(session.getMentorId())))
                .toList();
    }

    public List<SessionResponseDTO> getAllSessionsForAdmin() {
        return repo.findAllByOrderByIdDesc().stream()
                .map(session -> map(session, fetchUser(session.getUserId()), fetchUser(session.getMentorId())))
                .toList();
    }

    public void deleteSession(Long id, Long actingUserId) {
        Session session = repo.findById(id)
                .orElseThrow(() -> new CustomException("Session not found"));

        if (actingUserId == null) {
            throw new CustomException("Authenticated user is required");
        }

        if (actingUserId.equals(session.getUserId())) {
            repo.delete(session);
            return;
        }

        assertMentorOwnsSession(session, actingUserId);
        if (!"COMPLETED".equals(session.getStatus()) && !"REJECTED".equals(session.getStatus())) {
            throw new CustomException("Only completed or rejected sessions can be deleted");
        }

        repo.delete(session);
    }

    public Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long value) {
            return value;
        }
        if (principal instanceof Integer value) {
            return value.longValue();
        }
        if (principal instanceof String value) {
            return Long.parseLong(value);
        }

        return null;
    }

    private void assertMentorOwnsSession(Session session, Long actingMentorId) {
        if (actingMentorId == null) {
            throw new CustomException("Authenticated mentor is required");
        }

        if (!actingMentorId.equals(session.getMentorId())) {
            throw new CustomException("Only the assigned mentor can manage this session");
        }
    }

    private SessionResponseDTO map(Session s, UserDTO userProfile) {
        return map(s, userProfile, null);
    }

    private SessionResponseDTO map(Session s, UserDTO userProfile, UserDTO mentorProfile) {
        return SessionResponseDTO.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .mentorId(s.getMentorId())
                .status(s.getStatus())
                .timeSlot(s.getTimeSlot())
                .meetingUrl(s.getMeetingUrl())
                .durationMinutes(s.getDurationMinutes())
                .sessionPrice(s.getSessionPrice())
                .userProfile(userProfile)
                .mentorProfile(mentorProfile)
                .build();
    }

    private UserDTO fetchUser(Long userId) {
        try {
            return authClient.getUserById(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildAcceptedSessionEmail(UserDTO user, UserDTO mentor, Session session) {
        String timeSlot = session.getTimeSlot() == null ? "Scheduled by mentor" : session.getTimeSlot();
        return """
                Hi %s,

                Your session has been successfully booked!

                Mentor Details
                Name: %s
                Email: %s

                Session Details
                Date/Time: %s
                Duration: %s minutes
                Price: $%s

                Join Session
                Click the link below to join your session:
                %s

                Important Instructions
                - Join 5 minutes before the session starts
                - Ensure a stable internet connection
                - Keep your questions ready

                Need Help?
                If you face any issues, contact us at:
                support@skillsync.com

                Best regards,
                Team SkillSync
                """.formatted(
                displayName(user),
                displayName(mentor),
                mentor == null ? "Not available" : mentor.getEmail(),
                timeSlot,
                session.getDurationMinutes() == null ? 30 : session.getDurationMinutes(),
                session.getSessionPrice() == null ? "0" : session.getSessionPrice().toPlainString(),
                session.getMeetingUrl() == null ? "Meeting link will be shared soon." : session.getMeetingUrl()
        );
    }

    private String buildRejectedSessionEmail(UserDTO user, UserDTO mentor, Session session) {
        return """
                Hi %s,

                Your session request was rejected.

                Mentor Details
                Name: %s
                Email: %s

                Session Details
                Duration: %s minutes
                Price: $%s

                You can book another mentor or try a different time from the Sessions page.

                Need Help?
                If you face any issues, contact us at:
                support@skillsync.com

                Best regards,
                Team SkillSync
                """.formatted(
                displayName(user),
                displayName(mentor),
                mentor == null ? "Not available" : mentor.getEmail(),
                session.getDurationMinutes() == null ? 30 : session.getDurationMinutes(),
                session.getSessionPrice() == null ? "0" : session.getSessionPrice().toPlainString()
        );
    }

    private String displayName(UserDTO user) {
        if (user == null) {
            return "SkillSync user";
        }
        return user.getName() == null || user.getName().isBlank() ? user.getEmail() : user.getName();
    }
}
