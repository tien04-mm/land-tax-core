package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.domain.service.TaxCalculationService;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AttachmentEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxPaymentEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AttachmentJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxPaymentJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxDeclarationRepository;
import com.thanglong.landtax.usecase.dto.TaxDeclarationRequest;
import com.thanglong.landtax.usecase.dto.TaxDeclarationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Use case xử lý nộp tờ khai thuế đất.
 *
 * <p><b>Luồng xử lý:</b></p>
 * <ol>
 *   <li>Lấy cccd_number từ JWT (SecurityContext) → gọi SyncUserFromVneidUseCase → citizen_id</li>
 *   <li>Tìm thửa đất (land_parcels) theo parcel_id</li>
 *   <li>Phát hiện gian lận: so sánh declared_area vs area_size (ngưỡng 2%)</li>
 *   <li>Tính thuế: Diện tích × Đơn giá đất × Thuế suất</li>
 *   <li>Lưu tờ khai vào bảng records (category=TAX_DECLARATION, status=PENDING/WARNING_FRAUD)</li>
 *   <li>Tạo bản ghi tax_payments với số tiền thuế tính được</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class SubmitDeclarationUseCase {

    private final SyncUserFromVneidUseCase syncUserFromVneidUseCase;
    private final TaxCalculationService taxCalculationService;
    private final LandParcelJpaRepository landParcelJpaRepository;
    private final RecordJpaRepository recordJpaRepository;
    private final TaxPaymentJpaRepository taxPaymentJpaRepository;
    private final TaxDeclarationRepository taxDeclarationRepository;
    private final AttachmentJpaRepository attachmentJpaRepository;
    private final AuditLogService auditLogService;

    /**
     * Nộp tờ khai thuế đất.
     *
     * @param request DTO chứa: parcelId, attachmentIds
     * @return TaxDeclarationResponse với kết quả xử lý
     */
    @Transactional
    public TaxDeclarationResponse submitDeclaration(TaxDeclarationRequest request) {

        // ===== BƯỚC 1: Lấy citizen_id từ JWT =====
        String cccdNumber = SecurityContextHolder.getContext().getAuthentication().getName();
        Integer citizenId = syncUserFromVneidUseCase.syncAndGetCitizenId(cccdNumber);
        int currentYear = LocalDate.now().getYear();

        log.info("Submit declaration - CCCD: {}, citizenId: {}, parcelId: {}", cccdNumber, citizenId, request.getParcelId());

        // ===== BƯỚC 2: Tìm thửa đất & Validate quyền sở hữu =====
        LandParcelEntity parcel = landParcelJpaRepository.findById(request.getParcelId())
                .orElseThrow(() -> new RuntimeException("Thửa đất không tồn tại: " + request.getParcelId()));

        if (parcel.getOwnerCccd() == null || !parcel.getOwnerCccd().equals(cccdNumber)) {
            log.warn("Cảnh báo bảo mật: CCCD {} cố nộp tờ khai cho thửa đất {} không thuộc sở hữu", cccdNumber, request.getParcelId());
            throw new RuntimeException("Bạn không có quyền nộp tờ khai cho thửa đất này.");
        }

        // ===== BƯỚC 3: Tính thuế tự động =====
        TaxCalculationService.TaxCalculationResult taxResult =
                taxCalculationService.calculateTax(
                        request.getParcelId(),
                        citizenId,
                        currentYear
                );

        // ===== BƯỚC 4: Lưu tờ khai vào bảng records =====
        RecordEntity record = RecordEntity.builder()
                .citizenId(citizenId)
                .landParcelId(request.getParcelId())
                .recordCategory("TAX_DECLARATION")
                .currentStatus("SUBMITTED")
                .build();

        RecordEntity savedRecord = recordJpaRepository.save(record);
        log.info("Record created: recordId={}, status=SUBMITTED", savedRecord.getRecordId());

        // ===== BƯỚC 5: Tạo bản ghi tax_payments =====
        TaxPaymentEntity taxPayment = TaxPaymentEntity.builder()
                .recordId(savedRecord.getRecordId())
                .landParcelId(request.getParcelId())
                .taxYear(currentYear)
                .totalAmountDue(taxResult.getTaxAmount())
                .dueDate(LocalDate.of(currentYear, 12, 31))
                .lateFeeAmount(BigDecimal.ZERO)
                .paymentStatus("UNPAID")
                .build();

        TaxPaymentEntity savedPayment = taxPaymentJpaRepository.save(taxPayment);
        log.info("Tax payment created: payId={}, amount={} VNĐ", savedPayment.getPayId(), savedPayment.getTotalAmountDue());

        // ===== BƯỚC 6: Xử lý file đính kèm =====
        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            java.util.List<AttachmentEntity> attachments = attachmentJpaRepository.findAllById(request.getAttachmentIds());
            for (AttachmentEntity att : attachments) {
                att.setRelatedEntityId((long) savedRecord.getRecordId());
                att.setRelatedEntityType("RECORD");
            }
            attachmentJpaRepository.saveAll(attachments);
            log.info("Đã liên kết {} tài liệu đính kèm với recordId={}", attachments.size(), savedRecord.getRecordId());
        }

        // Lưu bản record TaxDeclaration cho lịch sử (History)
        com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxDeclarationEntity declaration = com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxDeclarationEntity.builder()
            .citizenId(citizenId)
            .senderCccd(cccdNumber)
            .parcelId(request.getParcelId())
            .taxYear(currentYear)
            .declaredArea(taxResult.getActualArea()) // Sử dụng luôn diện tích thực tế
            .actualArea(taxResult.getActualArea())
            .declaredPurpose(parcel.getUsageType()) // Lấy mục đích từ sổ đỏ
            .status("SUBMITTED")
            .calculatedTaxAmount(taxResult.getTaxAmount())
            .unitPrice(taxResult.getUnitPrice())
            .taxRate(taxResult.getExemptionRate()) // Lưu tạm mức giảm trừ vào cột này hoặc log.
            .supportingDocuments(request.getAttachmentIds() != null ? request.getAttachmentIds().toString() : null)
            .build();
        taxDeclarationRepository.save(declaration);

        // Ghi log hệ thống
        auditLogService.log("SUBMIT_DECLARATION", "TAX_DECLARATION", 
            String.valueOf(savedRecord.getRecordId()), 
            "Công dân nộp hồ sơ khai thuế cho thửa đất " + request.getParcelId());

        // ===== BƯỚC 7: Trả về kết quả =====
        return TaxDeclarationResponse.builder()
                .recordId(savedRecord.getRecordId())
                .citizenId(citizenId)
                .parcelId(request.getParcelId())
                .taxYear(currentYear)
                .declaredArea(taxResult.getActualArea())
                .actualArea(taxResult.getActualArea())
                .declaredPurpose(parcel.getUsageType())
                .status("SUBMITTED")
                .calculatedTaxAmount(taxResult.getTaxAmount())
                .unitPrice(taxResult.getUnitPrice())
                .taxRate(taxResult.getExemptionRate())
                .submittedAt(savedRecord.getSubmittedAt())
                .build();
    }
}
