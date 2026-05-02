package com.lpu.sessionservice.dto;


import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionRequestDTO {

    private Long userId;
    private Long mentorId;
    private Integer durationMinutes;
    private BigDecimal sessionPrice;

    public SessionRequestDTO(Long userId, Long mentorId) {
        this.userId = userId;
        this.mentorId = mentorId;
    }
}
