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
        return ResponseEntity.ok(Map.of("message", "Dong bo ho so thanh cong", "profile", profile));
    }

    @org.springframework.web.bind.annotation.GetMapping
    public ResponseEntity<com.thanglong.landtax.usecase.dto.ProfileResponse> getProfile() {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(profileService.getProfile(cccd));
    }

    @org.springframework.web.bind.annotation.GetMapping("/me")
    public ResponseEntity<com.thanglong.landtax.usecase.dto.ProfileResponse> getProfileMe() {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(profileService.getProfileMe(cccd));
    }
}

