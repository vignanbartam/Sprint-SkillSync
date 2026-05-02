package com.lpu.authservice.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MentorSummaryDTO {
    private Long id;
    private String name;
    private String email;
    private Integer age;
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
