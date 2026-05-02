package com.lpu.authservice.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lpu.authservice.dto.MentorSummaryDTO;
import com.lpu.authservice.dto.UserDTO;
import com.lpu.authservice.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRoleAndMentorApprovedTrueOrderByNameAsc(String role);

    @Query("select u.role from User u where u.email = :email")
    Optional<String> findRoleByEmail(@Param("email") String email);

    @Query("select u.id from User u where u.email = :email")
    Optional<Long> findIdByEmail(@Param("email") String email);

    @Query("select u.password from User u where u.email = :email")
    Optional<String> findPasswordByEmail(@Param("email") String email);

    @Query("""
            select new com.lpu.authservice.dto.UserDTO(
                u.id,
                u.email,
                u.role,
                u.name,
                u.age,
                u.dob,
                u.address,
                u.phoneNumber,
                u.about,
                u.linkedinUrl,
                u.xUrl,
                u.instagramUrl,
                null,
                u.quickSessionPrice,
                u.focusedSessionPrice,
                u.deepSessionPrice,
                null,
                u.mentorApproved,
                u.profilePictureData is not null,
                u.biodataData is not null,
                u.biodataFileName
            )
            from User u
            where u.id = :id
            """)
    Optional<UserDTO> findUserDtoById(@Param("id") Long id);

    @Query("""
            select new com.lpu.authservice.dto.UserDTO(
                u.id,
                u.email,
                u.role,
                u.name,
                u.age,
                u.dob,
                u.address,
                u.phoneNumber,
                u.about,
                u.linkedinUrl,
                u.xUrl,
                u.instagramUrl,
                null,
                u.quickSessionPrice,
                u.focusedSessionPrice,
                u.deepSessionPrice,
                null,
                u.mentorApproved,
                u.profilePictureData is not null,
                u.biodataData is not null,
                u.biodataFileName
            )
            from User u
            where u.email = :email
            """)
    Optional<UserDTO> findUserDtoByEmail(@Param("email") String email);

    @Query("""
            select new com.lpu.authservice.dto.MentorSummaryDTO(
                u.id,
                u.name,
                u.email,
                u.age,
                u.about,
                u.linkedinUrl,
                u.xUrl,
                u.instagramUrl,
                null,
                u.quickSessionPrice,
                u.focusedSessionPrice,
                u.deepSessionPrice,
                null,
                u.mentorApproved,
                u.profilePictureData is not null,
                u.biodataData is not null,
                u.biodataFileName
            )
            from User u
            where u.role = :role and u.mentorApproved = true
            order by u.name asc
            """)
    List<MentorSummaryDTO> findMentorSummaries(@Param("role") String role);

    @Query("""
            select new com.lpu.authservice.dto.UserDTO(
                u.id,
                u.email,
                u.role,
                u.name,
                u.age,
                u.dob,
                u.address,
                u.phoneNumber,
                u.about,
                u.linkedinUrl,
                u.xUrl,
                u.instagramUrl,
                null,
                u.quickSessionPrice,
                u.focusedSessionPrice,
                u.deepSessionPrice,
                null,
                u.mentorApproved,
                u.profilePictureData is not null,
                u.biodataData is not null,
                u.biodataFileName
            )
            from User u
            order by u.id desc
            """)
    List<UserDTO> findAllUserDtos();
}
