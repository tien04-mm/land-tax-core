package com.thanglong.landtax.usecase.dto;

import lombok.*;


/**
 * Các DTO liên quan đến xác thực và vai trò.
 */
public class AuthRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QrLoginRequest {
        private String qrToken;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SwitchRoleRequest {
        private String targetRole;
    }
}
