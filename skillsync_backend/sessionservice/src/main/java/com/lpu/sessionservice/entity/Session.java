package com.lpu.sessionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sessions")

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long mentorId;

    private String status; // PENDING, ACCEPTED, REJECTED
    private String timeSlot;
    private String meetingUrl;
    private Integer durationMinutes;

    @Column(precision = 10, scale = 2)
    private BigDecimal sessionPrice;
}
