package com.thanglong.landtax.usecase.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * DTO nhan thong tin dinh danh cong dan tu VNeID Auth Service.
 * Mapping voi response cua: GET /api/vneid/internal/citizens/{cccd}
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
