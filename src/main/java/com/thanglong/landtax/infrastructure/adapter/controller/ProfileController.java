package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.usecase.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/sync")
    public ResponseEntity<?> syncProfile() {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity profile = profileService.syncProfileFromVneid(cccd);
        return ResponseEntity.ok(Map.of("message", "Đồng bộ hồ sơ thành công", "profile", profile));
    }
}
