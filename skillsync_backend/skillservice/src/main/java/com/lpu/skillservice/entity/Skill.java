package com.lpu.skillservice.entity;



import jakarta.persistence.*;
import lombok.*;

@Entity


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
}
