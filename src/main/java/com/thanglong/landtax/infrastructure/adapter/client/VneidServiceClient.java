package com.thanglong.landtax.infrastructure.adapter.client;

import com.thanglong.landtax.usecase.dto.ApiResponse;
import com.thanglong.landtax.usecase.dto.VneidQrStatusResponse;
import com.thanglong.landtax.usecase.dto.VneidAuthResponse;
import com.thanglong.landtax.usecase.dto.AuthRequest;
import com.thanglong.landtax.usecase.dto.CitizenIdentityDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * OpenFeign client de goi sang VNeID Auth Service (Port 9090).
 */
@FeignClient(name = "vneid-auth-service", url = "${vneid.service.url}")
public interface VneidServiceClient {

    /**
     * Lay thong tin dinh danh cong dan tu VNeID theo so CCCD.
     */
    @GetMapping("/api/vneid/internal/citizens/{cccd}")
    ApiResponse<CitizenIdentityDTO> getCitizenByCccd(
            @PathVariable("cccd") String cccdNumber,
            @RequestHeader("X-Internal-Secret") String secret);

    @PutMapping("/api/vneid/internal/citizens/{cccd}/status")
    ApiResponse<Void> updateCitizenStatus(
            @PathVariable("cccd") String cccdNumber, 
            @RequestParam("active") boolean active,
            @RequestHeader("X-Internal-Secret") String secret);

    @PutMapping("/api/vneid/internal/citizens/{cccd}/role")
    ApiResponse<Void> updateCitizenRole(
            @PathVariable("cccd") String cccdNumber, 
            @RequestParam("role") String role,
            @RequestHeader("X-Internal-Secret") String secret);

    /**
     * Kiem tra trang thai ma QR tu VNeID Auth Service.
     */
    @GetMapping("/api/auth/qr-status")
    ApiResponse<VneidQrStatusResponse> getQrStatus(@RequestParam("token") String qrToken);

    /**
     * Handshake cuoi cung: Dang nhap QR tai VNeID de lay CCCD.
     */
    @PostMapping("/api/auth/qr-login")
    ApiResponse<VneidAuthResponse> loginByQr(@RequestBody AuthRequest.QrLoginRequest request);
}
