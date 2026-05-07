package com.thanglong.landtax.usecase.dto;

import lombok.*;

import java.util.List;

/**
 * DTO phản hồi xác thực chuẩn hóa.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private Integer userId;
    private String fullName;
    private String cccdNumber;
    private String activeRole;
    private List<String> roles;
}
