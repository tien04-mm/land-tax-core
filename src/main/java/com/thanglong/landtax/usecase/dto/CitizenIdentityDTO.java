package com.thanglong.landtax.usecase.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * DTO nhận thông tin định danh công dân từ VNeID Auth Service.
 * Mapping với response của: GET /api/vneid/internal/citizens/{cccd}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenIdentityDTO {

    private String cccdNumber;
    private String fullName;
    private LocalDate dob;
    private String gender;
    private String email;
    private String phoneNumber;
}
