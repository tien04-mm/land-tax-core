package com.thanglong.landtax.usecase.dto;

import lombok.*;

/**
 * DTO nhận phản hồi Auth từ VNeID Auth Service (Port 9090).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VneidAuthResponse {
    private String token;
    private String tokenType;
    private String userId; // Chứa số CCCD trả về từ 9090
    private String cccdNumber;
    private String fullName;
    private String email;
    private String role;
    private String message;
}
