package com.zeezaglobal.prescription.DTO;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChangePasswordDTO {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    public ChangePasswordDTO() {}

    public ChangePasswordDTO(String currentPassword, String newPassword, String confirmPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

}