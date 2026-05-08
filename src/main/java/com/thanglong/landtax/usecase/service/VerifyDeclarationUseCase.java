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
public class VerifyDeclarationUseCase {

    private final RecordJpaRepository recordJpaRepository;
    private final TaxDeclarationRepository taxDeclarationRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public Map<String, Object> verifyDeclaration(Integer recordId) {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("LAND_OFFICER {} đang xác minh hồ sơ {}", cccd, recordId);

        // 1. Lấy hồ sơ
        RecordEntity record = recordJpaRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Hồ sơ không tồn tại: " + recordId));

        // 2. Kiểm tra trạng thái
        if (!"SUBMITTED".equals(record.getCurrentStatus())) {
            throw new RuntimeException("Chỉ có thể xác minh hồ sơ đang ở trạng thái SUBMITTED. Trạng thái hiện tại: " + record.getCurrentStatus());
        }

        // 3. Cập nhật hồ sơ
        record.setCurrentStatus("VERIFIED");
        recordJpaRepository.save(record);

        // 4. Tìm và cập nhật TaxDeclarationEntity
        List<TaxDeclarationEntity> declarations = taxDeclarationRepository
                .findByCitizenIdAndParcelIdAndStatus(record.getCitizenId(), record.getLandParcelId(), "SUBMITTED");

        if (!declarations.isEmpty()) {
            TaxDeclarationEntity declaration = declarations.get(0);
            declaration.setStatus("VERIFIED");
            taxDeclarationRepository.save(declaration);
            log.info("Đã cập nhật trạng thái tờ khai {} sang VERIFIED", declaration.getId());
        }

        // 5. Ghi Audit Log
        auditLogService.log("VERIFY_DECLARATION", "TAX_DECLARATION", 
            String.valueOf(recordId), 
            "Cán bộ địa chính " + cccd + " đã xác minh hồ sơ " + recordId + " là hợp lệ");

        return Map.of(
            "recordId", recordId,
            "status", "VERIFIED",
            "message", "Hồ sơ đã được xác minh hợp lệ thành công"
        );
    }
}
