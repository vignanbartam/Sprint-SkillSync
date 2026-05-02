package com.lpu.authservice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.lpu.authservice.config.RabbitConfig;
import com.lpu.authservice.dto.NotificationEvent;
import com.lpu.authservice.entity.MentorApplication;
import com.lpu.authservice.entity.User;
import com.lpu.authservice.exception.CustomException;
import com.lpu.authservice.repository.MentorApplicationRepository;
import com.lpu.authservice.repository.UserRepository;

@Service
public class MentorApplicationService {

    @Autowired
    private MentorApplicationRepository repo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public String apply(MentorApplication app) {
        if (app.getUserId() == null) {
            throw new CustomException("UserId is required");
        }

        if (repo.findByUserIdOrderByIdDesc(app.getUserId()).stream().anyMatch(existing -> "PENDING".equals(existing.getStatus()))) {
            throw new CustomException("You already have a pending mentor application");
        }

        if (repo.findByUserIdOrderByIdDesc(app.getUserId()).stream().anyMatch(existing -> "APPROVED".equals(existing.getStatus()))) {
            throw new CustomException("You are already approved as a mentor");
        }

        if (app.getSkillIds() == null || app.getSkillIds().isEmpty()) {
            throw new CustomException("Select at least one skill before applying");
        }

        if (app.getExperience() <= 0) {
            throw new CustomException("Experience must be greater than 0");
        }

        User applicant = userRepo.findById(app.getUserId())
                .orElseThrow(() -> new CustomException("User not found"));
        if (applicant.getBiodataData() == null || applicant.getBiodataData().length == 0) {
            throw new CustomException("Biodata is required before applying as a mentor");
        }

        app.setStatus("PENDING");
        repo.save(app);
        return "Application Submitted";
    }

    public List<MentorApplication> getAllApplications() {
        return repo.findByStatusOrderByIdDesc("PENDING");
    }

    public String approve(Long appId) {

        MentorApplication app = repo.findById(appId)
                .orElseThrow(() -> new CustomException("Application not found"));

        if (!"PENDING".equals(app.getStatus())) {
            throw new CustomException("Only pending applications can be approved");
        }

        User user = userRepo.findById(app.getUserId())
                .orElseThrow(() -> new CustomException("User not found"));
        
        if (user.getRole().equals("ROLE_ADMIN")) {
            throw new CustomException("Admin cannot be converted to mentor");
        }

        if (user.getBiodataData() == null || user.getBiodataData().length == 0) {
            throw new CustomException("Biodata is required before approving this mentor");
        }

        user.setRole("ROLE_MENTOR");
        user.setMentorApproved(true);

        app.setStatus("APPROVED");

        userRepo.save(user);
        notifyUser(user, "Mentor application approved", """
                Hi %s,

                Your mentor application has been approved.

                You can now manage mentor pricing from your profile and receive session requests from learners.

                Best regards,
                Team SkillSync
                """.formatted(displayName(user)), "MENTOR_APPLICATION_APPROVED");

        return "Mentor Approved";
    }

    public String reject(Long appId) {
        return reject(appId, "Your application did not meet the current mentor requirements.");
    }

    public String reject(Long appId, String reason) {
        MentorApplication app = repo.findById(appId)
                .orElseThrow(() -> new CustomException("Application not found"));

        if (!"PENDING".equals(app.getStatus())) {
            throw new CustomException("Only pending applications can be rejected");
        }

        User user = userRepo.findById(app.getUserId())
                .orElseThrow(() -> new CustomException("User not found"));

        repo.delete(app);
        notifyUser(user, "Mentor application update", """
                Hi %s,

                Your mentor application was not approved.

                Reason:
                %s

                You may update your profile and apply again when ready.

                Best regards,
                Team SkillSync
                """.formatted(displayName(user), normalizeReason(reason)), "MENTOR_APPLICATION_REJECTED");

        return "Mentor Application Rejected";
    }

    public String deleteUser(Long userId, String reason) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new CustomException("User not found"));

        if ("ROLE_ADMIN".equals(user.getRole())) {
            throw new CustomException("Admin users cannot be deleted from this screen");
        }

        notifyUser(user, "SkillSync account removed", """
                Hi %s,

                Your SkillSync account has been removed by an administrator.

                Reason:
                %s

                If you believe this was a mistake, contact support@skillsync.com.

                Best regards,
                Team SkillSync
                """.formatted(displayName(user), normalizeReason(reason)), "ADMIN_USER_DELETED");

        repo.findAll().stream()
                .filter(app -> userId.equals(app.getUserId()))
                .forEach(repo::delete);
        userRepo.delete(user);

        return "User deleted and notified";
    }

    public List<MentorApplication> getApplicationsForUser(Long userId) {
        return repo.findByUserIdOrderByIdDesc(userId);
    }

    public String updateMentorSkills(Long userId, List<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            throw new CustomException("Select at least one skill");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new CustomException("User not found"));
        if (!"ROLE_MENTOR".equals(user.getRole()) || !user.isMentorApproved()) {
            throw new CustomException("Only approved mentors can update mentor skills");
        }

        MentorApplication application = repo.findFirstByUserIdAndStatusOrderByIdDesc(userId, "APPROVED")
                .orElseGet(() -> MentorApplication.builder()
                        .userId(userId)
                        .experience(1)
                        .status("APPROVED")
                        .build());

        application.setSkillIds(skillIds);
        repo.save(application);
        return "Mentor skills updated";
    }

    public String revokeApplication(Long appId, Long userId) {
        MentorApplication app = repo.findById(appId)
                .orElseThrow(() -> new CustomException("Application not found"));

        if (!userId.equals(app.getUserId())) {
            throw new CustomException("You can revoke only your own application");
        }

        if (!"PENDING".equals(app.getStatus())) {
            throw new CustomException("Only pending applications can be revoked");
        }

        repo.delete(app);
        return "Application revoked";
    }

    public String updateUserRole(Long userId, String role) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new CustomException("User not found"));

        if ("ROLE_ADMIN".equals(user.getRole())) {
            throw new CustomException("Admin role cannot be changed here");
        }

        if (!"ROLE_USER".equals(role) && !"ROLE_MENTOR".equals(role)) {
            throw new CustomException("Invalid role");
        }

        user.setRole(role);
        user.setMentorApproved("ROLE_MENTOR".equals(role));
        userRepo.save(user);
        return "Role updated";
    }

    private void notifyUser(User user, String subject, String message, String type) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE,
                    RabbitConfig.ROUTING_KEY,
                    NotificationEvent.builder()
                            .userId(user.getId())
                            .email(user.getEmail())
                            .subject(subject)
                            .message(message)
                            .type(type)
                            .build()
            );
        } catch (Exception e) {
            System.out.println("Notification failed: " + e.getMessage());
        }
    }

    private String displayName(User user) {
        return user.getName() == null || user.getName().isBlank() ? user.getEmail() : user.getName();
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? "No reason was provided." : reason.trim();
    }
}
