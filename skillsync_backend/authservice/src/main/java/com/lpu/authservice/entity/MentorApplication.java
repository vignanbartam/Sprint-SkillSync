package com.lpu.authservice.entity;



import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private List<Long> skillIds;
    private int experience;
    

    private String status; // PENDING, APPROVED, REJECTED
}
