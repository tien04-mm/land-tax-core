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

    public CitizenLocalEntity getProfile(String cccdNumber) {
        return citizenLocalJpaRepository.findByCccdNumber(cccdNumber)
                .orElseThrow(() -> new RuntimeException("Khong tim thay profile cho CCCD: " + cccdNumber));
    }
}
