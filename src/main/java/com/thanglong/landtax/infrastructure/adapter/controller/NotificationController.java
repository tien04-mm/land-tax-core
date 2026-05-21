package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.NotificationEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
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
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;
    private final AccountJpaRepository accountJpaRepository;

    @Operation(summary = "Xem hop thu thong bao", description = "Nguoi dan xem cac thong bao he thong gui den")
    @GetMapping("/me")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<NotificationEntity>> getMyNotifications() {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();

        // CCCD -> CitizenId -> AccountId -> Notifications
        Integer accountId = citizenLocalJpaRepository.findByCccdNumber(cccd)
                .flatMap(citizen -> accountJpaRepository.findByCitizenId(citizen.getCitizenId()))
                .map(account -> account.getAccountId())
                .orElse(null);

        if (accountId == null) {
            return ResponseEntity.ok(List.of());
        }

        List<NotificationEntity> notifications = notificationJpaRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        return ResponseEntity.ok(notifications);
    }
}
