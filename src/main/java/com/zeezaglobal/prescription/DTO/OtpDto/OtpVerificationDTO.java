package com.zeezaglobal.prescription.DTO.OtpDto;

import lombok.Data;

@Data
public class OtpVerificationDTO {
    private String email;
    private String otp;
}
