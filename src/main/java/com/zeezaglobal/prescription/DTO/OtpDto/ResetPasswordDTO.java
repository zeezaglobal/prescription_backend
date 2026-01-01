package com.zeezaglobal.prescription.DTO.OtpDto;

import lombok.Data;

@Data
public class ResetPasswordDTO {
    private String token;
    private String newPassword;
    private String confirmPassword;
}