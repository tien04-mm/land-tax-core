package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AuditLogEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AuditLogJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AuditLogService {

    private final AuditLogJpaRepository auditLogRepository;

    /**
     * Ghi nh t k  thao t c (Audit Trail)
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

            AuditLogEntity logEntity = AuditLogEntity.builder()
                    .userCccd(cccd)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .description(description)
                    .ipAddress(ipAddress)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(logEntity);
            log.info("[AUDIT] {} - {} tr n {} {}: {}", cccd, action, targetType, targetId, description);

        } catch (Exception e) {
            log.error("L i khi ghi audit log th  c ng: {}", e.getMessage());
        }
    }
}

