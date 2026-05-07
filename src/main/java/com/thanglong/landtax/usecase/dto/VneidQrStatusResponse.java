package com.thanglong.landtax.usecase.dto;

import lombok.*;

/**
 * DTO nhận trạng thái QR từ VNeID Auth Service.
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
