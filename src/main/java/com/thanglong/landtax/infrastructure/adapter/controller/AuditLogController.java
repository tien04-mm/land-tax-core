package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.usecase.dto.AuditLogResponseDTO;
import com.thanglong.landtax.usecase.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN', 'TAX_OFFICER', 'LAND_OFFICER', 'CITIZEN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * API lấy danh sách lịch sử thao tác, lọc theo action_type và actor_id.
     */
    @GetMapping
    public ResponseEntity<List<AuditLogResponseDTO>> getAuditLogs(
            @RequestParam(value = "action_type", required = false) String actionType,
            @RequestParam(value = "actor_id", required = false) Integer actorId) {
        log.info("GET /api/admin/logs - action_type={}, actor_id={}", actionType, actorId);
        List<AuditLogResponseDTO> logs = auditLogService.getProcessingLogs(actionType, actorId);
        return ResponseEntity.ok(logs);
    }
}
