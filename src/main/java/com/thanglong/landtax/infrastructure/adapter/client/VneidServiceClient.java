package com.thanglong.landtax.infrastructure.adapter.client;

import com.thanglong.landtax.usecase.dto.ApiResponse;
import com.thanglong.landtax.usecase.dto.VneidQrStatusResponse;
import com.thanglong.landtax.usecase.dto.VneidAuthResponse;
import com.thanglong.landtax.usecase.dto.AuthRequest;
import com.thanglong.landtax.usecase.dto.CitizenIdentityDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * OpenFeign client để gọi sang VNeID Auth Service (Port 9090).
 */
@FeignClient(name = "vneid-auth-service", url = "${vneid.service.url}")
public interface VneidServiceClient {

    /**
     * Lấy thông tin định danh công dân từ VNeID theo số CCCD.
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
     * Kiểm tra trạng thái mã QR từ VNeID Auth Service.
     */
    @GetMapping("/api/auth/qr-status")
    ApiResponse<VneidQrStatusResponse> getQrStatus(@RequestParam("token") String qrToken);

    /**
     * Handshake cuối cùng: Đăng nhập QR tại VNeID để lấy CCCD.
     */
    @PostMapping("/api/auth/qr-login")
    ApiResponse<VneidAuthResponse> loginByQr(@RequestBody AuthRequest.QrLoginRequest request);
}
