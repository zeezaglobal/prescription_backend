package com.zeezaglobal.prescription.DTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePrescriptionStatusDTO {


    private String status; // PENDING, ACTIVE, COMPLETED, CANCELLED, EXPIRED

    private String reason; // Optional reason for status change
}
