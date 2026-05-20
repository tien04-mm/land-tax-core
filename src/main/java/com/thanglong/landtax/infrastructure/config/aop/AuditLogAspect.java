package com.thanglong.landtax.infrastructure.config.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class AuditLogAspect {

    @AfterReturning("@annotation(com.thanglong.landtax.infrastructure.config.aop.AuditLog)")
    public void logAuditActivity(JoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            AuditLog auditLogAnnotation = signature.getMethod().getAnnotation(AuditLog.class);
            String action = auditLogAnnotation.action();

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

            log.info("[AUDIT LOG] user: {}, action: {}, ip: {}", cccd, action, ipAddress);

        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }
}

