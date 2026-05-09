package com.thanglong.landtax.usecase.dto;

import lombok.*;


/**
 * Cac DTO lien quan den xac thuc va vai tro.
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
