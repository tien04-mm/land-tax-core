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
@SuppressWarnings("null")
public class ExemptionService {

        private final TaxExemptSubjectJpaRepository exemptionRepository;
        private final CitizenLocalJpaRepository citizenRepository;
        private final AccountJpaRepository accountRepository;

        public TaxExemptSubjectEntity createExemption(String uploaderCccd, Map<String, Object> request) {
                String citizenCccd = (String) request.get("citizen_cccd");
                String reason = (String) request.get("exemption_reason");
                Number exemptionRate = (Number) request.get("exemption_rate");
                Integer appliedYear = (Integer) request.get("applied_year");

                log.info("C n b  {} t o mi n gi m cho c ng d n {}", uploaderCccd, citizenCccd);

                // 1. T m th ng tin c n b upload
                CitizenLocalEntity uploaderCitizen = citizenRepository.findByCccdNumber(uploaderCccd)
                                .orElseThrow(() -> new IllegalArgumentException("Kh ng t m th y th ng tin c n b "));
                AccountEntity uploaderAccount = accountRepository.findByCitizenId(uploaderCitizen.getCitizenId())
                                .orElseThrow(() -> new IllegalArgumentException("C n b  ch a c  t i kho n"));

                // 2. T m th ng tin c ng d n c mi n gi m
                CitizenLocalEntity citizen = citizenRepository.findByCccdNumber(citizenCccd)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Kh ng t m th y th ng tin c ng d n (citizen_cccd)"));

                // 3. T o b n ghi mi n gi m
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
