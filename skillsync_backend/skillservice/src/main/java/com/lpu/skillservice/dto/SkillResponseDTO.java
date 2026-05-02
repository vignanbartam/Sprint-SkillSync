package com.lpu.skillservice.dto;



import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillResponseDTO {

    private Long id;
    private String name;
    private String description;
}
