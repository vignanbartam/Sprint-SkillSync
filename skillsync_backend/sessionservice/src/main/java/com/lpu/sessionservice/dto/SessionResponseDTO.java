package com.lpu.sessionservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionResponseDTO {

    private Long id;
    private Long userId;
    private Long mentorId;
    private String status;
    private String timeSlot;
    private String meetingUrl;
    private Integer durationMinutes;
    private BigDecimal sessionPrice;
    private UserDTO userProfile;
    private UserDTO mentorProfile;
}
