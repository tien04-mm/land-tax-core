package com.thanglong.landtax.usecase.dto;

import lombok.*;

/**
 * DTO nhan trang thai QR tu VNeID Auth Service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VneidQrStatusResponse {
    private String status;
    private String token;
}
