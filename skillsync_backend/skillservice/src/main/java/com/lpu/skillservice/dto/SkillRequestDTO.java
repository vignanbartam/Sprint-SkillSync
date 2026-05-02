package com.lpu.skillservice.dto;



import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillRequestDTO {

    private String name;
    private String description;
}
