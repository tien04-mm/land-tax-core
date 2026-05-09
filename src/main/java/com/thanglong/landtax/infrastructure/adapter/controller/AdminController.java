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
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.client.VneidServiceClient;
import com.thanglong.landtax.usecase.service.AuditLogService;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AuditLogJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AuditLogEntity;

/**
 * Controller cho cac API thong ke bao cao (Dashboard) danh cho Can bo / Admin.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminController {

    private final StatisticsService statisticsService;
    private final AccountJpaRepository accountJpaRepository;
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;
    private final VneidServiceClient vneidServiceClient;
    private final AuditLogService auditLogService;
    private final AuditLogJpaRepository auditLogJpaRepository;

    @org.springframework.beans.factory.annotation.Value("${internal.api.secret:VNeIDInternalSecretKey2025}")
    private String internalSecret;

    /**
     * API Thong kA tong quan cho Dashboard.
     * YAu cau quyon ADMIN hoac TAX_OFFICER.
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'TAX_OFFICER')")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics() {
        Map<String, Object> stats = statisticsService.getDashboardStatistics();
        return ResponseEntity.ok(stats);
    }

    @PutMapping("/users/{cccd}/status")
    @PreAuthorize("hasRole('ADMIN')")
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
        
        auditLogService.log("UPDATE_USER_STATUS", "ACCOUNT", cccd, "Cap nhat trang thai active = " + active);

        return ResponseEntity.ok(Map.of("message", "Cap nhat trang thai nguoi dAng th nh cAng"));
    }

    @PutMapping("/users/{cccd}/role")
    @PreAuthorize("hasRole('ADMIN')")
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

        auditLogService.log("UPDATE_USER_ROLE", "ACCOUNT", cccd, "Cap nhat role = " + role);

        return ResponseEntity.ok(Map.of("message", "Cap nhat role nguoi dAng th nh cAng"));
    }

    /**
     * GET /api/admin/audit-logs - Xem nhat ky he thong (Audit Trail).
     * YAu cau quyon ROLE_ADMIN.
     */
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(required = false) String userCccd,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        
        List<AuditLogEntity> logs = 
            auditLogJpaRepository.findWithFilters(userCccd, action, fromDate, toDate);

        return ResponseEntity.ok(Map.of(
            "total", logs.size(),
            "data", logs
        ));
    }
}

