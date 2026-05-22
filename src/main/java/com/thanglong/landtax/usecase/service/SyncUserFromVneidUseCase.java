package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.client.VneidServiceClient;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AccountEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.usecase.dto.ApiResponse;
import com.thanglong.landtax.usecase.dto.CitizenIdentityDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case ng b th ng tin c ng d n t VNeID Auth Service sang
 * land_tax_management.
 *
 * <p>
 * <b>Lu ng x l :</b>
 * </p>
 * <ol>
 * <li>Ki m tra cccd_number t n t i trong b ng citizens c c b ch a</li>
 * <li>N u t n t i tr v citizen_id (INT) ngay</li>
 * <li>N u ch a t n t i g i VneidServiceClient l y th ng tin t VNeID</li>
 * <li>INSERT v o b ng citizens l y citizen_id t t ng</li>
 * <li>INSERT v o b ng accounts v i role_id = 2 (CITIZEN)</li>
 * <li>C 2 INSERT n m trong @Transactional rollback n u l i</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class SyncUserFromVneidUseCase {

    private final VneidServiceClient vneidServiceClient;
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;
    private final AccountJpaRepository accountJpaRepository;

    @org.springframework.beans.factory.annotation.Value("${internal.api.secret:VNeIDInternalSecretKey2025}")
    private String internalSecret;

    /** role_id = 2 t ng ng v i vai tr CITIZEN trong b ng roles */
    private static final int ROLE_CITIZEN = 2;

    /**
     * ng b c ng d n t VNeID d a tr n s CCCD.
     *
     * <p>
     * N u CCCD t n t i trong DB c c b , tr v citizen_id hi n c .
     * N u ch a, g i VNeID Internal API l y th ng tin, sau INSERT
     * v o c b ng citizens v accounts trong c ng m t transaction.
     * </p>
     *
     * @param cccdNumber S C n c c c ng d n (12 s ) tr ch xu t t JWT
     * @return citizen_id (INT) c c b trong b ng land_tax_management.citizens
     */
    @Transactional
    public Integer syncAndGetCitizenId(String cccdNumber) {
        // ===== B C 1: G i VNeID Internal API l y th ng tin m i nh t =====
        log.info("Fetching latest info from VNeID: CCCD={}", cccdNumber);

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
            throw new RuntimeException(
                    "Kh ng th  l y th ng tin c ng d n t  VNeID cho CCCD: " + cccdNumber, e);
        }

        if (vneidData == null) {
            throw new RuntimeException("VNeID tr  v  d  li u r ng cho CCCD: " + cccdNumber);
        }

        // ===== B C 2: Ki m tra CCCD t n t i trong DB c c b ch a =====
        var existingCitizenOpt = citizenLocalJpaRepository.findByCccdNumber(cccdNumber);
        if (existingCitizenOpt.isPresent()) {
            CitizenLocalEntity existingCitizen = existingCitizenOpt.get();
            // C p nh t th ng tin t  VNeID
            existingCitizen.setFullName(java.util.Optional.ofNullable(vneidData.getFullName()).orElse(existingCitizen.getFullName()));
            if (vneidData.getEmail() != null) existingCitizen.setEmail(vneidData.getEmail());
            if (vneidData.getPhoneNumber() != null) existingCitizen.setPhoneNumber(vneidData.getPhoneNumber());
            
            citizenLocalJpaRepository.save(existingCitizen);
            
            log.info("Citizen already exists locally, updated info: CCCD={}, citizenId={}",
                    cccdNumber, existingCitizen.getCitizenId());
            return existingCitizen.getCitizenId();
        }

        // ===== B C 3: INSERT v o b ng citizens =====
        CitizenLocalEntity newCitizen = CitizenLocalEntity.builder()
                .cccdNumber(java.util.Optional.ofNullable(vneidData.getCccdNumber()).orElse(cccdNumber))
                .fullName(java.util.Optional.ofNullable(vneidData.getFullName()).orElse("Unknown"))
                .email(vneidData.getEmail())
                .phoneNumber(vneidData.getPhoneNumber())
                .build();

        CitizenLocalEntity savedCitizen = citizenLocalJpaRepository.save(newCitizen);
        Integer newCitizenId = savedCitizen.getCitizenId();

        log.info("Inserted citizen into local DB: CCCD={}, citizenId={}", cccdNumber, newCitizenId);

        // ===== B C 4: INSERT v o b ng accounts v i role_id = 2 (CITIZEN) =====
        AccountEntity newAccount = AccountEntity.builder()
                .citizenId(newCitizenId)
                .roleId(ROLE_CITIZEN)
                .accountStatus("ACTIVE")
                .build();

        AccountEntity savedAccount = accountJpaRepository.save(newAccount);

        log.info("Inserted account for citizen: citizenId={}, accountId={}, roleId={}",
                newCitizenId, savedAccount.getAccountId(), ROLE_CITIZEN);

        return newCitizenId;
    }
}
