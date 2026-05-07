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
 * Use case xử lý duyệt tờ khai thuế.
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
                .orElseThrow(() -> new RuntimeException("Bản ghi thuế không tồn tại: " + taxRecordId));

        if (!"PENDING".equals(taxRecord.getStatus())) {
            throw new RuntimeException("Chỉ có thể duyệt tờ khai ở trạng thái PENDING");
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
                .orElseThrow(() -> new RuntimeException("Bản ghi thuế không tồn tại: " + taxRecordId));

        taxRecord.setStatus("REJECTED");
        taxRecord.setNotes(reason);
        taxRecord.setUpdatedAt(LocalDateTime.now());

        TaxRecord saved = taxRecordRepository.save(taxRecord);
        log.info("Tax record rejected: id={}, reason={}", saved.getId(), reason);

        return taxRecordMapper.toDto(saved);
    }
}
