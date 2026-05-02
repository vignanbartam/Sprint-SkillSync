package com.lpu.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    private Long userId;
    private String email;
    private String subject;
    private String message;
    private String type;
    private String timeSlot;
    private String meetingUrl;
}
