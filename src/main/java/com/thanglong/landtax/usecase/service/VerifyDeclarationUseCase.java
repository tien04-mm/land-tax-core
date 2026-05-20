package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxDeclarationEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxDeclarationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class VerifyDeclarationUseCase {

    private final RecordJpaRepository recordJpaRepository;
    private final TaxDeclarationRepository taxDeclarationRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public Map<String, Object> verifyDeclaration(Integer recordId) {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("LAND_OFFICER {}  ang x c minh h  s  {}", cccd, recordId);

        // 1. L y h  s 
        RecordEntity record = recordJpaRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("H  s  kh ng t n t i: " + recordId));

        // 2. Ki m tra tr ng th i
        if (!"SUBMITTED".equals(record.getCurrentStatus())) {
            throw new RuntimeException("Ch  c  th  x c minh h  s   ang   tr ng th i SUBMITTED. Tr ng th i hi n t i: " + record.getCurrentStatus());
        }

        // 3. C p nh t h  s 
        record.setCurrentStatus("VERIFIED");
        recordJpaRepository.save(record);

        // 4. T m v  c p nh t TaxDeclarationEntity
        List<TaxDeclarationEntity> declarations = taxDeclarationRepository
                .findByCitizenIdAndParcelIdAndStatus(record.getCitizenId(), record.getLandParcelId(), "SUBMITTED");

        if (!declarations.isEmpty()) {
            TaxDeclarationEntity declaration = declarations.get(0);
            declaration.setStatus("VERIFIED");
            taxDeclarationRepository.save(declaration);
            log.info("  c p nh t tr ng th i t  khai {} sang VERIFIED", declaration.getDeclarationId());
        }

        // 5. Ghi Audit Log
        auditLogService.log("VERIFY_DECLARATION", "TAX_DECLARATION", 
            String.valueOf(recordId), 
            "C n b   a ch nh " + cccd + "   x c minh h  s  " + recordId + " l  h p l ");

        return Map.of(
            "recordId", recordId,
            "status", "VERIFIED",
            "message", "H  s     c x c minh h p l  th nh c ng"
        );
    }
}


