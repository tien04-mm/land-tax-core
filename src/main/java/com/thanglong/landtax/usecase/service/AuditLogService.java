package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.ProcessingLogJpaRepository;
import com.thanglong.landtax.usecase.dto.AuditLogResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AuditLogService {

    private final ProcessingLogJpaRepository processingLogJpaRepository;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository citizenLocalJpaRepository;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository accountJpaRepository;

    /**
     * Lấy lịch sử thao tác (Audit Logs) từ bảng processing_logs
     */
    public List<AuditLogResponseDTO> getProcessingLogs(String actionType, Integer actorId) {
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
            var citizen = citizenLocalJpaRepository.findByCccdNumber(cccd)
                    .orElseThrow(() -> new com.thanglong.landtax.infrastructure.adapter.controller.exception.ResourceNotFoundException("Công dân không tồn tại"));
            var account = accountJpaRepository.findByCitizenId(citizen.getCitizenId())
                    .orElseThrow(() -> new com.thanglong.landtax.infrastructure.adapter.controller.exception.ResourceNotFoundException("Tài khoản không tồn tại"));
            actorId = account.getAccountId();
        }

        log.info("Fetching processing logs for actionType={}, actorId={}", actionType, actorId);
        return processingLogJpaRepository.filterLogs(actionType, actorId).stream()
                .map(entity -> AuditLogResponseDTO.builder()
                        .plogId(entity.getPlogId())
                        .recordId(entity.getRecordId())
                        .processorAccountId(entity.getProcessorAccountId())
                        .processingStep(entity.getProcessingStep())
                        .oldStatus(entity.getOldStatus())
                        .newStatus(entity.getNewStatus())
                        .processorNotes(entity.getProcessorNotes())
                        .processedAt(entity.getProcessedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Ghi nhat ky thao tac (Audit Trail)
     */
    public void log(String action, String targetType, String targetId, String description) {
        try {
            String cccd = "SYSTEM";
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                cccd = SecurityContextHolder.getContext().getAuthentication().getName();
            }

            String ipAddress = "UNKNOWN";
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = request.getRemoteAddr();
            }

            log.info("[AUDIT] {} - {} tren {} {}: {} (IP: {})", cccd, action, targetType, targetId, description, ipAddress);

        } catch (Exception e) {
            log.error("Loi khi ghi audit log: {}", e.getMessage());
        }
    }
}

