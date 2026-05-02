package com.lpu.authservice.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.security.SecureRandom;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.lpu.authservice.dto.BinaryFileDTO;
import com.lpu.authservice.dto.MentorSummaryDTO;
import com.lpu.authservice.dto.NotificationEvent;
import com.lpu.authservice.dto.ProfileUpdateDTO;
import com.lpu.authservice.dto.UserDTO;
import com.lpu.authservice.dto.VerifyRegistrationDTO;
import com.lpu.authservice.entity.MentorApplication;
import com.lpu.authservice.config.RabbitConfig;
import com.lpu.authservice.entity.User;
import com.lpu.authservice.repository.MentorApplicationRepository;
import com.lpu.authservice.exception.CustomException;
import com.lpu.authservice.repository.UserRepository;
import com.lpu.authservice.security.JwtUtil;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private MentorApplicationRepository mentorApplicationRepo;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // REGISTER
    public String register(User user) {
        validateRegistration(user);

        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            throw new CustomException("Email already exists");
        }

        String otp = String.valueOf(100000 + secureRandom.nextInt(900000));
        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole("ROLE_USER");
        pendingRegistrations.put(user.getEmail().toLowerCase(), new PendingRegistration(user, otp));

        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE,
                    RabbitConfig.ROUTING_KEY,
                    NotificationEvent.builder()
                            .email(user.getEmail())
                            .subject("Verify your SkillSync email")
                            .message("""
                                    Hi %s,

                                    Use this OTP to verify your SkillSync account:
                                    %s

                                    This code is required to complete registration.

                                    Best regards,
                                    Team SkillSync
                                    """.formatted(user.getName(), otp))
                            .type("REGISTRATION_OTP")
                            .build()
            );
            return "OTP sent to your email.";
        } catch (Exception e) {
            pendingRegistrations.remove(user.getEmail().toLowerCase());
            throw new CustomException("Unable to send OTP email. Please make sure RabbitMQ and notificationservice are running.");
        }
    }

    public String verifyRegistration(VerifyRegistrationDTO dto) {
        if (dto.getEmail() == null || dto.getOtp() == null) {
            throw new CustomException("Email and OTP are required");
        }

        PendingRegistration pending = pendingRegistrations.get(dto.getEmail().toLowerCase());
        if (pending == null || !pending.otp().equals(dto.getOtp().trim())) {
            throw new CustomException("Invalid OTP");
        }

        userRepo.save(pending.user());
        pendingRegistrations.remove(dto.getEmail().toLowerCase());
        return "Registered successfully";
    }

    // LOGIN
    public String login(String email, String password) {

        Long userId = userRepo.findIdByEmail(email)
                .orElseThrow(() -> new CustomException("User not found"));
        String encodedPassword = userRepo.findPasswordByEmail(email)
                .orElseThrow(() -> new CustomException("User not found"));
        String role = userRepo.findRoleByEmail(email)
                .orElseThrow(() -> new CustomException("User not found"));

        if (!encoder.matches(password, encodedPassword)) {
            throw new CustomException("Invalid password");
        }

        return jwtUtil.generateToken(
                userId,
                email,
                role
        );
    }

    public UserDTO getUserById(Long id) {
        UserDTO user = userRepo.findUserDtoById(id)
                .orElseThrow(() -> new CustomException("User not found"));
        return attachUserSkills(user);
    }

    @Transactional(readOnly = true)
    public User getUserEntityById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new CustomException("User not found"));
    }

    public UserDTO getCurrentProfile(String email) {
        UserDTO user = userRepo.findUserDtoByEmail(email)
                .orElseThrow(() -> new CustomException("User not found"));
        return attachUserSkills(user);
    }

    @Transactional
    public UserDTO updateProfile(String email, ProfileUpdateDTO dto) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found"));

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new CustomException("Name is required");
        }

        if (dto.getAge() == null || dto.getAge() <= 0) {
            throw new CustomException("Age must be greater than 0");
        }

        user.setName(dto.getName().trim());
        user.setAge(dto.getAge());
        user.setDob(dto.getDob());
        user.setAddress(dto.getAddress());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setAbout(dto.getAbout());
        user.setLinkedinUrl(dto.getLinkedinUrl());
        user.setXUrl(dto.getXUrl());
        user.setInstagramUrl(dto.getInstagramUrl());
        if ("ROLE_MENTOR".equals(user.getRole())) {
            user.setQuickSessionPrice(dto.getQuickSessionPrice());
            user.setFocusedSessionPrice(dto.getFocusedSessionPrice());
            user.setDeepSessionPrice(dto.getDeepSessionPrice());
        }

        return mapUser(userRepo.save(user));
    }

    @Transactional
    public UserDTO uploadProfilePicture(String email, MultipartFile file) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found"));

        if (file == null || file.isEmpty()) {
            throw new CustomException("Profile picture file is required");
        }

        try {
            user.setProfilePictureData(file.getBytes());
            user.setProfilePictureContentType(file.getContentType());
            user.setProfilePictureFileName(file.getOriginalFilename());
        } catch (IOException e) {
            throw new CustomException("Unable to upload profile picture");
        }

        return mapUser(userRepo.save(user));
    }

    @Transactional
    public UserDTO uploadBiodata(String email, MultipartFile file) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found"));

        if ("ROLE_ADMIN".equals(user.getRole())) {
            throw new CustomException("Admin biodata upload is not supported");
        }

        if (file == null || file.isEmpty()) {
            throw new CustomException("Biodata file is required");
        }

        if (!isSupportedBiodata(file)) {
            throw new CustomException("Biodata must be a PDF, DOC, or DOCX file");
        }

        try {
            user.setBiodataData(file.getBytes());
            user.setBiodataContentType(file.getContentType());
            user.setBiodataFileName(file.getOriginalFilename());
        } catch (IOException e) {
            throw new CustomException("Unable to upload biodata");
        }

        return mapUser(userRepo.save(user));
    }

    public List<MentorSummaryDTO> getAllMentors() {
        return userRepo.findMentorSummaries("ROLE_MENTOR").stream()
                .map(this::attachMentorSkills)
                .toList();
    }

    public List<UserDTO> getAllUsersForAdmin() {
        return userRepo.findAllUserDtos().stream()
                .map(this::attachUserSkills)
                .toList();
    }

    @Transactional
    public UserDTO updateOwnRole(String email, String role) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found"));

        if ("ROLE_ADMIN".equals(user.getRole())) {
            throw new CustomException("Admin role cannot be changed here");
        }

        if (!"ROLE_USER".equals(role) && !"ROLE_MENTOR".equals(role)) {
            throw new CustomException("Invalid role");
        }

        user.setRole(role);
        user.setMentorApproved("ROLE_MENTOR".equals(role));
        return mapUser(userRepo.save(user));
    }

    @Transactional(readOnly = true)
    public BinaryFileDTO getProfilePicture(Long id) {
        User user = getUserEntityById(id);
        if (user.getProfilePictureData() == null || user.getProfilePictureData().length == 0) {
            throw new CustomException("Profile picture not found");
        }

        return BinaryFileDTO.builder()
                .data(user.getProfilePictureData())
                .contentType(user.getProfilePictureContentType())
                .fileName(user.getProfilePictureFileName())
                .build();
    }

    @Transactional(readOnly = true)
    public BinaryFileDTO getBiodata(Long id) {
        User user = getUserEntityById(id);
        if (user.getBiodataData() == null || user.getBiodataData().length == 0) {
            throw new CustomException("Biodata not found");
        }

        return BinaryFileDTO.builder()
                .data(user.getBiodataData())
                .contentType(user.getBiodataContentType())
                .fileName(user.getBiodataFileName())
                .build();
    }

    private void validateRegistration(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            throw new CustomException("Name is required");
        }

        if (user.getAge() == null || user.getAge() <= 0) {
            throw new CustomException("Age must be greater than 0");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new CustomException("Email is required");
        }

        if (!EMAIL_PATTERN.matcher(user.getEmail().trim()).matches()) {
            throw new CustomException("Enter a valid email address");
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new CustomException("Password is required");
        }
    }

    private UserDTO mapUser(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .name(user.getName())
                .age(user.getAge())
                .dob(user.getDob())
                .address(user.getAddress())
                .phoneNumber(user.getPhoneNumber())
                .about(user.getAbout())
                .linkedinUrl(user.getLinkedinUrl())
                .xUrl(user.getXUrl())
                .instagramUrl(user.getInstagramUrl())
                .mentorExperience(mentorExperience(user.getId()))
                .quickSessionPrice(user.getQuickSessionPrice())
                .focusedSessionPrice(user.getFocusedSessionPrice())
                .deepSessionPrice(user.getDeepSessionPrice())
                .skillIds(mentorSkillIds(user.getId()))
                .mentorApproved(user.isMentorApproved())
                .hasProfilePicture(user.getProfilePictureData() != null && user.getProfilePictureData().length > 0)
                .hasBiodata(user.getBiodataData() != null && user.getBiodataData().length > 0)
                .biodataFileName(user.getBiodataFileName())
                .build();
    }

    private MentorSummaryDTO attachMentorSkills(MentorSummaryDTO mentor) {
        mentor.setSkillIds(mentorSkillIds(mentor.getId()));
        mentor.setMentorExperience(mentorExperience(mentor.getId()));
        return mentor;
    }

    private UserDTO attachUserSkills(UserDTO user) {
        user.setSkillIds(mentorSkillIds(user.getId()));
        user.setMentorExperience(mentorExperience(user.getId()));
        return user;
    }

    private List<Long> mentorSkillIds(Long userId) {
        if (userId == null) {
            return List.of();
        }

        return mentorApplicationRepo.findFirstByUserIdAndStatusOrderByIdDesc(userId, "APPROVED")
                .map(application -> application.getSkillIds() == null ? List.<Long>of() : application.getSkillIds())
                .orElse(List.of());
    }

    private Integer mentorExperience(Long userId) {
        if (userId == null) {
            return null;
        }

        return mentorApplicationRepo.findFirstByUserIdAndStatusOrderByIdDesc(userId, "APPROVED")
                .map(MentorApplication::getExperience)
                .orElse(null);
    }

    private boolean isSupportedBiodata(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        if (contentType != null && (
                contentType.equals("application/pdf")
                || contentType.equals("application/msword")
                || contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
            return true;
        }

        if (fileName == null) {
            return false;
        }

        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".pdf")
                || lowerFileName.endsWith(".doc")
                || lowerFileName.endsWith(".docx");
    }

    private record PendingRegistration(User user, String otp) {}
}
