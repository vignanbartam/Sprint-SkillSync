package com.lpu.authservice.dto;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String email;
    private String role;
    private String name;
    private Integer age;
    private LocalDate dob;
    private String address;
    private String phoneNumber;
    private String about;
    private String linkedinUrl;
    private String xUrl;
    private String instagramUrl;
    private Integer mentorExperience;
    private BigDecimal quickSessionPrice;
    private BigDecimal focusedSessionPrice;
    private BigDecimal deepSessionPrice;
    private List<Long> skillIds;
    private boolean mentorApproved;
    private boolean hasProfilePicture;
    private boolean hasBiodata;
    private String biodataFileName;
}
