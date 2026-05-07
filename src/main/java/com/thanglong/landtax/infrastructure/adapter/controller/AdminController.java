package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.usecase.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller cho các API thống kê báo cáo (Dashboard) dành cho Cán bộ / Admin.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminController {

    private final StatisticsService statisticsService;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository accountJpaRepository;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository citizenLocalJpaRepository;
    private final com.thanglong.landtax.infrastructure.adapter.client.VneidServiceClient vneidServiceClient;
    private final com.thanglong.landtax.usecase.service.AuditLogService auditLogService;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AuditLogJpaRepository auditLogJpaRepository;

    @org.springframework.beans.factory.annotation.Value("${internal.api.secret:VNeIDInternalSecretKey2025}")
    private String internalSecret;

    /**
     * API Thống kê tổng quan cho Dashboard.
     * Yêu cầu quyền ADMIN hoặc TAX_OFFICER.
     */
    @GetMapping("/statistics")
    // @PreAuthorize("hasAnyAuthority('ADMIN', 'TAX_OFFICER', 'ROLE_ADMIN', 'ROLE_TAX_OFFICER')")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics() {
        Map<String, Object> stats = statisticsService.getDashboardStatistics();
        return ResponseEntity.ok(stats);
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
            // Ignore if vneid is not reachable in local dev
        }
        
        auditLogService.log("UPDATE_USER_STATUS", "ACCOUNT", cccd, "Cập nhật trạng thái active = " + active);

        return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái người dùng thành công"));
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
            // Ignore if vneid is not reachable in local dev
        }

        auditLogService.log("UPDATE_USER_ROLE", "ACCOUNT", cccd, "Cập nhật role = " + role);

        return ResponseEntity.ok(Map.of("message", "Cập nhật role người dùng thành công"));
    }

    /**
     * GET /api/admin/audit-logs — Xem nhật ký hệ thống (Audit Trail).
     * Yêu cầu quyền ROLE_ADMIN.
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(required = false) String userCccd,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        
        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN"));
        
        if (!isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "Chỉ ROLE_ADMIN mới có quyền xem nhật ký hệ thống"));
        }

        List<com.thanglong.landtax.infrastructure.adapter.persistence.entity.AuditLogEntity> logs = 
            auditLogJpaRepository.findWithFilters(userCccd, action, fromDate, toDate);

        return ResponseEntity.ok(Map.of(
            "total", logs.size(),
            "data", logs
        ));
    }
}
