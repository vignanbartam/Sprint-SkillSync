package com.lpu.notificationservice.dto;

import lombok.*;

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
