package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.controller.exception.ResourceNotFoundException;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxDeclarationEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxDeclarationRepository;
import com.thanglong.landtax.usecase.dto.ForwardRecordRequest;
import com.thanglong.landtax.usecase.dto.RecordRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class RecordService {

    private final RecordJpaRepository recordJpaRepository;
    private final TaxDeclarationRepository taxDeclarationRepository;

    /**
     * Tao ho so moi theo chuan 3NF.
     * Luu vao bang records truoc, lay ID, sau do tu dong khoi tao va luu tiep TaxDeclaration.
     */
    @Transactional
    public RecordEntity createRecord(RecordRequestDTO request) {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Creating record for citizenId={}, landParcelId={}, cccd={}",
                request.getCitizenId(), request.getLandParcelId(), cccd);

        // 1. Luu vao bang records truoc
        RecordEntity record = RecordEntity.builder()
                .citizenId(request.getCitizenId())
                .landParcelId(request.getLandParcelId())
                .recordCategory(request.getRecordCategory() != null ? request.getRecordCategory() : "TAX_DECLARATION")
                .currentStatus("SUBMITTED")
                .build();

        RecordEntity savedRecord = recordJpaRepository.save(record);
        log.info("Record created: recordId={}", savedRecord.getRecordId());

        // 2. Neu co du lieu ke khai thue, tao TaxDeclaration tu dong
        if (request.getTaxDeclaration() != null) {
            RecordRequestDTO.TaxDeclarationDTO taxDto = request.getTaxDeclaration();
            TaxDeclarationEntity declaration = TaxDeclarationEntity.builder()
                    .recordId(savedRecord.getRecordId())
                    .citizenId(request.getCitizenId())
                    .senderCccd(cccd)
                    .parcelId(request.getLandParcelId())
                    .taxYear(java.time.LocalDate.now().getYear())
                    .declaredArea(taxDto.getDeclaredArea())
                    .declaredPurpose(taxDto.getDeclaredPurpose())
                    .phoneNumber(taxDto.getPhoneNumber())
                    .address(taxDto.getAddress())
                    .status("SUBMITTED")
                    .build();

            taxDeclarationRepository.save(declaration);
            log.info("TaxDeclaration created for recordId={}", savedRecord.getRecordId());
        }

        // 3. Reload entity to include relationship
        return recordJpaRepository.findById(savedRecord.getRecordId()).orElse(savedRecord);
    }

    @Transactional
    public void forwardRecord(Integer recordId, ForwardRecordRequest request) {
        log.info("Forwarding record: recordId={}, note={}", recordId, request.getForwardNote());

        // 1. Tìm kiếm Record theo recordId
        RecordEntity record = recordJpaRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ không tồn tại"));

        String oldStatus = record.getCurrentStatus();

        // 2. Kiểm tra trạng thái hiện tại
        if ("VERIFIED".equals(oldStatus) || "APPROVED".equals(oldStatus) || "REJECTED".equals(oldStatus)) {
            throw new IllegalArgumentException("Không thể luân chuyển hồ sơ ở trạng thái này");
        }

        // 3. Cập nhật current_status của Record thành "VERIFIED"
        record.setCurrentStatus("VERIFIED");
        recordJpaRepository.save(record);

        // 4. Tìm và cập nhật TaxDeclarationEntity tương ứng
        TaxDeclarationEntity declaration = taxDeclarationRepository.findById(recordId).orElse(null);
        if (declaration == null) {
            // Dự phòng: Tìm theo các trường citizenId, parcelId, và status
            List<TaxDeclarationEntity> declarations = taxDeclarationRepository
                    .findByCitizenIdAndParcelIdAndStatus(record.getCitizenId(), record.getLandParcelId(), oldStatus);
            if (!declarations.isEmpty()) {
                declaration = declarations.get(0);
            }
        }

        if (declaration != null) {
            declaration.setStatus("VERIFIED");
            declaration.setReviewNote(request.getForwardNote());
            taxDeclarationRepository.save(declaration);
            log.info("Đã cập nhật trạng thái tờ khai {} sang VERIFIED và cập nhật reviewNote", declaration.getDeclarationId());
        }

        log.info("Record {} forwarded successfully from status {} to VERIFIED", recordId, oldStatus);
    }
}
