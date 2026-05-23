package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.client.VneidServiceClient;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.usecase.dto.ApiResponse;
import com.thanglong.landtax.usecase.dto.CitizenIdentityDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ProfileService {

    private final VneidServiceClient vneidServiceClient;
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository accountJpaRepository;

    @org.springframework.beans.factory.annotation.Value("${internal.api.secret:VNeIDInternalSecretKey2025}")
    private String internalSecret;

    @Transactional
    public CitizenLocalEntity syncProfileFromVneid(String cccdNumber) {
        log.info("B t  u  ng b  profile t  VNeID cho CCCD: {}", cccdNumber);

        CitizenIdentityDTO vneidData;
        try {
            ApiResponse<CitizenIdentityDTO> response = vneidServiceClient.getCitizenByCccd(cccdNumber, internalSecret);

            if (response == null || !response.isSuccess()) {
                throw new RuntimeException(
                        "VNeID tr  v  l i: " + (response != null ? response.getMessage() : "Unknown error"));
            }
            vneidData = response.getData();
        } catch (Exception e) {
            log.error("Failed to fetch citizen from VNeID: CCCD={}, error={}", cccdNumber, e.getMessage());
            throw new RuntimeException("Kh ng th  l y th ng tin c ng d n t  VNeID");
        }

        if (vneidData == null) {
            throw new RuntimeException("VNeID tr  v  d  li u r ng cho CCCD: " + cccdNumber);
        }

        CitizenLocalEntity citizen = citizenLocalJpaRepository.findByCccdNumber(cccdNumber)
                .orElse(new CitizenLocalEntity());

        citizen.setCccdNumber(cccdNumber);
        
        // VNeID l  Source of Truth: C p nh t n u c  d  li u, tr nh tr  ng h p ghi    b ng null
        if (org.springframework.util.StringUtils.hasText(vneidData.getFullName())) {
            citizen.setFullName(vneidData.getFullName());
        } else if (citizen.getFullName() == null) {
            citizen.setFullName("Unknown");
        }
        
        if (org.springframework.util.StringUtils.hasText(vneidData.getEmail())) {
            citizen.setEmail(vneidData.getEmail());
        }
        
        if (org.springframework.util.StringUtils.hasText(vneidData.getPhoneNumber())) {
            citizen.setPhoneNumber(vneidData.getPhoneNumber());
        }

        return citizenLocalJpaRepository.save(citizen);
    }

    public com.thanglong.landtax.usecase.dto.ProfileResponse getProfile(String cccdNumber) {
        return getProfileMe(cccdNumber);
    }

    public com.thanglong.landtax.usecase.dto.ProfileResponse getProfileMe(String cccdNumber) {
        CitizenLocalEntity citizen = citizenLocalJpaRepository.findByCccdNumber(cccdNumber)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy người dùng trong hệ thống thuế đất"));

        java.util.List<String> roles = accountJpaRepository.findRoleCodesByCccdNumber(cccdNumber);
        if (roles == null || roles.isEmpty()) {
            roles = java.util.List.of("ROLE_CITIZEN");
        }

        String activeRole = getMostPrivilegedRole(roles);

        return com.thanglong.landtax.usecase.dto.ProfileResponse.builder()
                .citizenId(citizen.getCitizenId())
                .cccdNumber(citizen.getCccdNumber())
                .fullName(citizen.getFullName())
                .email(citizen.getEmail())
                .phoneNumber(citizen.getPhoneNumber())
                .roles(roles)
                .activeRole(activeRole)
                .build();
    }

    private String getMostPrivilegedRole(java.util.List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "ROLE_CITIZEN";
        }
        String bestRole = roles.get(0);
        int maxWeight = getRoleWeight(bestRole);
        for (int i = 1; i < roles.size(); i++) {
            String role = roles.get(i);
            int weight = getRoleWeight(role);
            if (weight > maxWeight) {
                maxWeight = weight;
                bestRole = role;
            }
        }
        return bestRole;
    }

    private int getRoleWeight(String role) {
        if (role == null) return 0;
        switch (role) {
            case "ROLE_ADMIN": return 4;
            case "ROLE_TAX_OFFICER": return 3;
            case "ROLE_LAND_OFFICER": return 2;
            case "ROLE_CITIZEN": return 1;
            default: return 0;
        }
    }
}
