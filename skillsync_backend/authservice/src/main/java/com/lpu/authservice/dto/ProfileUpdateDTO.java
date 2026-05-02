package com.lpu.authservice.dto;

import java.time.LocalDate;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProfileUpdateDTO {
    private String name;
    private Integer age;
    private LocalDate dob;
    private String address;
    private String phoneNumber;
    private String about;
    private String linkedinUrl;
    private String xUrl;
    private String instagramUrl;
    private BigDecimal quickSessionPrice;
    private BigDecimal focusedSessionPrice;
    private BigDecimal deepSessionPrice;
}
