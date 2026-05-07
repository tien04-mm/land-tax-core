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
 * Use case đồng bộ thông tin công dân từ VNeID Auth Service sang land_tax_management.
 *
 * <p><b>Luồng xử lý:</b></p>
 * <ol>
 *   <li>Kiểm tra cccd_number đã tồn tại trong bảng citizens cục bộ chưa</li>
 *   <li>Nếu đã tồn tại → trả về citizen_id (INT) ngay</li>
 *   <li>Nếu chưa tồn tại → gọi VneidServiceClient lấy thông tin từ VNeID</li>
 *   <li>INSERT vào bảng citizens → lấy citizen_id tự tăng</li>
 *   <li>INSERT vào bảng accounts với role_id = 2 (CITIZEN)</li>
 *   <li>Cả 2 INSERT nằm trong @Transactional → rollback nếu lỗi</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncUserFromVneidUseCase {

    private final VneidServiceClient vneidServiceClient;
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;
    private final AccountJpaRepository accountJpaRepository;

    @org.springframework.beans.factory.annotation.Value("${internal.api.secret:VNeIDInternalSecretKey2025}")
    private String internalSecret;

    /** role_id = 2 tương ứng với vai trò CITIZEN trong bảng roles */
    private static final int ROLE_CITIZEN = 2;

    /**
     * Đồng bộ công dân từ VNeID dựa trên số CCCD.
     *
     * <p>Nếu CCCD đã tồn tại trong DB cục bộ, trả về citizen_id hiện có.
     * Nếu chưa, gọi VNeID Internal API để lấy thông tin, sau đó INSERT
     * vào cả bảng citizens và accounts trong cùng một transaction.</p>
     *
     * @param cccdNumber Số Căn cước công dân (12 số) trích xuất từ JWT
     * @return citizen_id (INT) cục bộ trong bảng land_tax_management.citizens
     */
    @Transactional
    public Integer syncAndGetCitizenId(String cccdNumber) {
        // ===== BƯỚC 1: Kiểm tra CCCD đã tồn tại trong DB cục bộ chưa =====
        var existingCitizen = citizenLocalJpaRepository.findByCccdNumber(cccdNumber);
        if (existingCitizen.isPresent()) {
            log.info("Citizen already exists locally: CCCD={}, citizenId={}",
                    cccdNumber, existingCitizen.get().getCitizenId());
            return existingCitizen.get().getCitizenId();
        }

        // ===== BƯỚC 2: Gọi VNeID Internal API lấy thông tin =====
        log.info("Citizen not found locally. Fetching from VNeID: CCCD={}", cccdNumber);

        CitizenIdentityDTO vneidData;
        try {
            ApiResponse<CitizenIdentityDTO> response = 
                    vneidServiceClient.getCitizenByCccd(cccdNumber, internalSecret);
            
            if (response == null || !response.isSuccess()) {
                throw new RuntimeException("VNeID trả về lỗi: " + (response != null ? response.getMessage() : "Unknown error"));
            }
            vneidData = response.getData();
        } catch (Exception e) {
            log.error("Failed to fetch citizen from VNeID: CCCD={}, error={}", cccdNumber, e.getMessage());
            throw new RuntimeException(
                    "Không thể lấy thông tin công dân từ VNeID cho CCCD: " + cccdNumber, e);
        }

        if (vneidData == null) {
            throw new RuntimeException("VNeID trả về dữ liệu rỗng cho CCCD: " + cccdNumber);
        }

        // ===== BƯỚC 3: INSERT vào bảng citizens =====
        CitizenLocalEntity newCitizen = CitizenLocalEntity.builder()
                .cccdNumber(java.util.Optional.ofNullable(vneidData.getCccdNumber()).orElse(cccdNumber))
                .fullName(java.util.Optional.ofNullable(vneidData.getFullName()).orElse("Unknown"))
                .dateOfBirth(vneidData.getDob())
                .gender(java.util.Optional.ofNullable(vneidData.getGender()).orElse("UNKNOWN"))
                .email(vneidData.getEmail())
                .phoneNumber(vneidData.getPhoneNumber())
                .build();

        CitizenLocalEntity savedCitizen = citizenLocalJpaRepository.save(newCitizen);
        Integer newCitizenId = savedCitizen.getCitizenId();

        log.info("Inserted citizen into local DB: CCCD={}, citizenId={}", cccdNumber, newCitizenId);

        // ===== BƯỚC 4: INSERT vào bảng accounts với role_id = 2 (CITIZEN) =====
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
