package com.thanglong.landtax.usecase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAdminDTO {
    private String cccdNumber;
    private String fullName;
    private String role;
    private String status;
    private String phoneNumber;
    private String email;
}
