package com.lpu.groupservice.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupMemberResponseDTO {
    private Long id;
    private Long groupId;
    private Long userId;
    private String displayName;
    private String status;
    private LocalDateTime joinedAt;
}
