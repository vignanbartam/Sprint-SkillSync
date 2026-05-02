package com.lpu.groupservice.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupMessageResponseDTO {
    private Long id;
    private Long groupId;
    private Long senderId;
    private String content;
    private LocalDateTime timestamp;
}
