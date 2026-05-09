package com.thanglong.landtax.usecase.dto;

import lombok.*;

/**
 * DTO nhan phan hoi Auth tu VNeID Auth Service (Port 9090).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VneidAuthResponse {
    private String token;
    private String tokenType;
    private String userId; // Chua so CCCD tra ve tu 9090
    private String cccdNumber;
    private String fullName;
    private String email;
    private String role;
    private String message;
}
