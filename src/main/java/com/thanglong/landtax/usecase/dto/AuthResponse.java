package com.thanglong.landtax.usecase.dto;

import lombok.*;

import java.util.List;

/**
 * DTO phan hoi xac thuc chuan hoa.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String refreshToken;
    private Integer userId;
    private String fullName;
    private String cccdNumber;
    private String activeRole;
    private List<String> roles;
}
