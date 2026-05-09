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
        citizen.setFullName(vneidData.getFullName());
        citizen.setDateOfBirth(vneidData.getDob());
        citizen.setGender(vneidData.getGender());
        citizen.setEmail(vneidData.getEmail());
        citizen.setPhoneNumber(vneidData.getPhoneNumber());

        if (citizen.getAddress() == null || citizen.getAddress().isEmpty()) {
            citizen.setAddress("H  N i (Mock Address t  VNeID)");
        }

        citizen.setLastSyncAt(LocalDateTime.now());
        citizen.setUpdatedAt(LocalDateTime.now());

        return citizenLocalJpaRepository.save(citizen);
    }
}
