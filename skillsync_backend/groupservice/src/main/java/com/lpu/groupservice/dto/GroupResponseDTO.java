package com.lpu.groupservice.dto;

import lombok.*;

@Data
@Builder
public class GroupResponseDTO {
    private Long id;
    private String name;
    private String description;
    private Long createdBy;
}
