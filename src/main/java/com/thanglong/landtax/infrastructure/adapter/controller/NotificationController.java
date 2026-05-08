package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.NotificationEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.NotificationJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationJpaRepository notificationJpaRepository;

    @Operation(summary = "Xem hộp thư thông báo", description = "Người dân xem các thông báo hệ thống gửi đến")
    @GetMapping("/me")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<NotificationEntity>> getMyNotifications() {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        List<NotificationEntity> notifications = notificationJpaRepository.findByCccdNumberOrderByCreatedAtDesc(cccd);
        return ResponseEntity.ok(notifications);
    }
}
