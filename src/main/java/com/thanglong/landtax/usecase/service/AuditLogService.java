package com.thanglong.landtax.usecase.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AuditLogService {

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

