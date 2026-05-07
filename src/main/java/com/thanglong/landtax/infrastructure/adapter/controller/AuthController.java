package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.usecase.dto.AuthRequest;
import com.thanglong.landtax.usecase.dto.AuthResponse;
import com.thanglong.landtax.usecase.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Quản lý đăng nhập và vai trò")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Đăng nhập bằng QR Token từ VNeID")
    @PostMapping("/qr-login")
    public ResponseEntity<?> qrLogin(@RequestBody AuthRequest.QrLoginRequest request) {
        try {
            AuthResponse response = authService.qrLogin(request.getQrToken());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đăng nhập thành công",
                "data", response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Chuyển đổi vai trò người dùng")
    @PostMapping("/switch-role")
    public ResponseEntity<?> switchRole(@RequestBody AuthRequest.SwitchRoleRequest request) {
        try {
            AuthResponse response = authService.switchRole(request.getTargetRole());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Chuyển đổi vai trò thành công",
                "data", response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
