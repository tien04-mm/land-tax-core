package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.usecase.service.StatisticsService;
import com.thanglong.landtax.usecase.service.AdminService;
import com.thanglong.landtax.usecase.dto.CreateUserRequest;
import com.thanglong.landtax.usecase.dto.UpdateRoleRequest;
import com.thanglong.landtax.usecase.dto.RoleDTO;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.client.VneidServiceClient;
import com.thanglong.landtax.usecase.service.AuditLogService;

/**
 * Controller cho cac API thong ke bao cao (Dashboard) va quan ly nguoi dung/role danh cho Admin.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SuppressWarnings("null")
@Slf4j
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final StatisticsService statisticsService;
    private final AdminService adminService;
    private final AccountJpaRepository accountJpaRepository;
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;
    private final VneidServiceClient vneidServiceClient;
    private final AuditLogService auditLogService;

    @org.springframework.beans.factory.annotation.Value("${internal.api.secret:VNeIDInternalSecretKey2025}")
    private String internalSecret;

    /**
     * API Thong ke tong quan cho Dashboard.
     */
    @GetMapping("/statistics/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics() {
        log.info("AdminController.getDashboardStatistics() hit");
        Map<String, Object> stats = statisticsService.getDashboardStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/admin/users - Danh sach nguoi dung.
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminService.getAllUsers(search));
    }

    /**
     * POST /api/admin/users - Tạo mới cán bộ/người dùng.
     */
    @PostMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        log.info("POST /api/admin/users - Admin creating user");
        try {
            CitizenLocalEntity user = adminService.createUser(request);
            auditLogService.log("CREATE_USER", "CITIZEN", request.getCccdNumber(), "Tạo cán bộ mới thành công: " + request.getFullName());
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            log.error("Lỗi khi tạo cán bộ: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/admin/roles - Xem danh sách Role.
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        log.info("GET /api/admin/roles - Admin fetching all roles");
        return ResponseEntity.ok(adminService.getAllRoles());
    }

    /**
     * PUT /api/admin/roles/{id} - Cập nhật Role.
     */
    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateRole(@PathVariable Integer id, @RequestBody UpdateRoleRequest request) {
        log.info("PUT /api/admin/roles/{} - Admin updating role", id);
        try {
            RoleDTO role = adminService.updateRole(id, request);
            auditLogService.log("UPDATE_ROLE", "ROLE", String.valueOf(id), "Cập nhật thông tin role thành công: " + request.getRoleName());
            return ResponseEntity.ok(role);
        } catch (RuntimeException e) {
            log.error("Lỗi khi cập nhật role: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{cccd}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable String cccd, @RequestParam boolean active) {
        var citizenOpt = citizenLocalJpaRepository.findByCccdNumber(cccd);
        if (citizenOpt.isPresent()) {
            Integer citizenId = citizenOpt.get().getCitizenId();
            var accountOpt = accountJpaRepository.findByCitizenId(citizenId);
            if (accountOpt.isPresent()) {
                var account = accountOpt.get();
                account.setAccountStatus(active ? "ACTIVE" : "LOCKED");
                accountJpaRepository.save(account);
            }
        }
        
        try {
            vneidServiceClient.updateCitizenStatus(cccd, active, internalSecret);
        } catch (Exception e) {
            log.warn("Failed to sync status to VNeID: {}", e.getMessage());
        }
        
        auditLogService.log("UPDATE_USER_STATUS", "ACCOUNT", cccd, "Cap nhat trang thai active = " + active);

        return ResponseEntity.ok(Map.of("message", "Cap nhat trang thai nguoi dung thanh cong"));
    }

    @PutMapping("/users/{cccd}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable String cccd, @RequestParam String role) {
        Integer roleId = switch (role) {
            case "ROLE_ADMIN" -> 1;
            case "ROLE_TAX_OFFICER" -> 3;
            case "ROLE_LAND_OFFICER" -> 4;
            default -> 2; // ROLE_CITIZEN
        };

        var citizenOpt = citizenLocalJpaRepository.findByCccdNumber(cccd);
        if (citizenOpt.isPresent()) {
            Integer citizenId = citizenOpt.get().getCitizenId();
            var accountOpt = accountJpaRepository.findByCitizenId(citizenId);
            if (accountOpt.isPresent()) {
                var account = accountOpt.get();
                account.setRoleId(roleId);
                accountJpaRepository.save(account);
            }
        }

        try {
            vneidServiceClient.updateCitizenRole(cccd, role, internalSecret);
        } catch (Exception e) {
            log.warn("Failed to sync role to VNeID: {}", e.getMessage());
        }

        auditLogService.log("UPDATE_USER_ROLE", "ACCOUNT", cccd, "Cap nhat role = " + role);

        return ResponseEntity.ok(Map.of("message", "Cap nhat role nguoi dung thanh cong"));
    }

    /**
     * GET /api/admin/audit-logs - Xem nhat ky he thong (Audit Trail).
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(required = false) String userCccd,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        
        return ResponseEntity.ok(Map.of(
            "total", 0,
            "data", List.of()
        ));
    }
}
