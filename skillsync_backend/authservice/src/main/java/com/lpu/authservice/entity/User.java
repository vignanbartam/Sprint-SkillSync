package com.lpu.authservice.entity;



import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;
    private String name;
    private Integer age;
    private LocalDate dob;
    private String address;
    private String phoneNumber;

    @Column(length = 1500)
    private String about;

    private String linkedinUrl;
    private String xUrl;
    private String instagramUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal quickSessionPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal focusedSessionPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal deepSessionPrice;

    private String role; // ROLE_USER, ROLE_ADMIN, ROLE_MENTOR
    

    private boolean mentorApproved;

    @Lob
    private byte[] profilePictureData;
    private String profilePictureContentType;
    private String profilePictureFileName;

    @Lob
    private byte[] biodataData;
    private String biodataContentType;
    private String biodataFileName;
}
