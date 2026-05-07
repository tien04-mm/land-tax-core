package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AccountEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxExemptSubjectEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxExemptSubjectJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExemptionService {

    private final TaxExemptSubjectJpaRepository exemptionRepository;
    private final CitizenLocalJpaRepository citizenRepository;
    private final AccountJpaRepository accountRepository;

    public TaxExemptSubjectEntity createExemption(String uploaderCccd, Map<String, Object> request) {
        String citizenCccd = (String) request.get("citizen_cccd");
        String reason = (String) request.get("exemption_reason");
        Number exemptionRate = (Number) request.get("exemption_rate");
        Integer appliedYear = (Integer) request.get("applied_year");

        log.info("Cán bộ {} tạo miễn giảm cho công dân {}", uploaderCccd, citizenCccd);

        // 1. Tìm thông tin cán bộ upload
        CitizenLocalEntity uploaderCitizen = citizenRepository.findByCccdNumber(uploaderCccd)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin cán bộ"));
        AccountEntity uploaderAccount = accountRepository.findByCitizenId(uploaderCitizen.getCitizenId())
                .orElseThrow(() -> new IllegalArgumentException("Cán bộ chưa có tài khoản"));

        // 2. Tìm thông tin công dân được miễn giảm
        CitizenLocalEntity citizen = citizenRepository.findByCccdNumber(citizenCccd)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin công dân (citizen_cccd)"));

        // 3. Tạo bản ghi miễn giảm
        TaxExemptSubjectEntity exemption = TaxExemptSubjectEntity.builder()
                .citizenId(citizen.getCitizenId())
                .fullName(citizen.getFullName())
                .exemptionReason(reason)
                .discountRate(new BigDecimal(exemptionRate.toString()))
                .appliedYear(appliedYear != null ? appliedYear : LocalDateTime.now().getYear())
                .uploadedByAccount(uploaderAccount.getAccountId())
                .uploadedAt(LocalDateTime.now())
                .build();

        return exemptionRepository.save(exemption);
    }
}
