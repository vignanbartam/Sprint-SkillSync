package com.lpu.authservice.dto;

import lombok.Data;

@Data
public class VerifyRegistrationDTO {
    private String email;
    private String otp;
}
