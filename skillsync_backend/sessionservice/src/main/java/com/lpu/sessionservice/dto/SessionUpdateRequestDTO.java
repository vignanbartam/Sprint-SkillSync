package com.lpu.sessionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionUpdateRequestDTO {
    private String timeSlot;
    private String meetingUrl;
}
