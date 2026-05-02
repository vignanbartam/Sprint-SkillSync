package com.lpu.sessionservice.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String email;
    private String role;
    private String name;
    private Integer age;
    private LocalDate dob;
    private String address;
    private String phoneNumber;
    private String about;
    private boolean mentorApproved;
    private boolean hasProfilePicture;
    private boolean hasBiodata;
    private String biodataFileName;
}
