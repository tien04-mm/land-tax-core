package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.domain.model.TaxRecord;
import com.thanglong.landtax.domain.repository.TaxRecordRepository;
import com.thanglong.landtax.usecase.dto.TaxDeclarationDTO;
import com.thanglong.landtax.usecase.mapper.TaxRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case xu ly duyet to khai thue.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApproveTaxUseCase {

    private final TaxRecordRepository taxRecordRepository;
    private final TaxRecordMapper taxRecordMapper;

    @Transactional
    public TaxDeclarationDTO approveTaxRecord(Long taxRecordId) {
        TaxRecord taxRecord = taxRecordRepository.findById(taxRecordId)
                .orElseThrow(() -> new RuntimeException("Tax record not found with ID: " + taxRecordId));

        if (!"PENDING".equals(taxRecord.getStatus())) {
            throw new RuntimeException("Only PENDING tax records can be approved");
        }

        taxRecord.setStatus("APPROVED");
        taxRecord.setApprovalDate(LocalDateTime.now());
        taxRecord.setUpdatedAt(LocalDateTime.now());

        TaxRecord saved = taxRecordRepository.save(taxRecord);
        log.info("Tax record approved: id={}", saved.getId());

        return taxRecordMapper.toDto(saved);
    }

    @Transactional
    public TaxDeclarationDTO rejectTaxRecord(Long taxRecordId, String reason) {
        TaxRecord taxRecord = taxRecordRepository.findById(taxRecordId)
                .orElseThrow(() -> new RuntimeException("Tax record not found with ID: " + taxRecordId));

        taxRecord.setStatus("REJECTED");
        taxRecord.setNotes(reason);
        taxRecord.setUpdatedAt(LocalDateTime.now());

        TaxRecord saved = taxRecordRepository.save(taxRecord);
        log.info("Tax record rejected: id={}, reason={}", saved.getId(), reason);

        return taxRecordMapper.toDto(saved);
    }
}
