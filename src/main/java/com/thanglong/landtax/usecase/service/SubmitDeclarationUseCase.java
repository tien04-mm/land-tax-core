package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.domain.service.TaxCalculationService;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxPaymentEntity;
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
 * Use case xu ly nop to khai thue dat.
 *
 * <p>
 * <b>Luong xu ly:</b>
 * </p>
 * <ol>
 * <li>Lay cccd_number tu JWT (SecurityContext) -> goi
 * SyncUserFromVneidUseCase -> citizen_id</li>
 * <li>Tim thua dat (land_parcels) theo parcel_id</li>
 * <li>Phat hien gian lan: so sanh declared_area vs area_size (nguong
 * 2%)</li>
 * <li>Tinh thue: Dien tich x Don gia dat</li>
 * <li>Luu to khai vao bang records (category=TAX_DECLARATION,
 * status=PENDING/WARNING_FRAUD)</li>
 * <li>Tao ban ghi tax_payments voi so tien thue tinh duoc</li>
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
    private final AuditLogService auditLogService;
    private final PaymentService paymentService;

    /**
     * Nop to khai thue dat.
     *
     * @param request DTO chua: parcelId, attachmentIds
     * @return TaxDeclarationResponse voi ket qua xu ly
     */
    @Transactional
    public TaxDeclarationResponse submitDeclaration(TaxDeclarationRequest request) {

        // ===== BUOC 1: Lay citizen_id tu JWT =====
        String cccdNumber = SecurityContextHolder.getContext().getAuthentication().getName();
        Integer citizenId = syncUserFromVneidUseCase.syncAndGetCitizenId(cccdNumber);
        int currentYear = LocalDate.now().getYear();

        log.info("Submit declaration - CCCD: {}, citizenId: {}, parcelId: {}", cccdNumber, citizenId,
                request.getParcelId());

        // ===== BUOC 2: Tim thua dat & Validate quyen so huu =====
        LandParcelEntity parcel = landParcelJpaRepository.findById(request.getParcelId())
                .orElseThrow(() -> new RuntimeException("Land parcel not found: " + request.getParcelId()));

        if (parcel.getOwnerCccd() == null || !parcel.getOwnerCccd().equals(cccdNumber)) {
            log.warn(
                    "Security Warning: CCCD {} attempted to submit declaration for parcel {} not owned by them",
                    cccdNumber, request.getParcelId());
            throw new RuntimeException("You do not have permission to submit a declaration for this land parcel.");
        }

        // ===== BUOC 3: Tinh thue tu dong =====
        TaxCalculationService.TaxCalculationResult taxResult = taxCalculationService.calculateTax(
                request.getParcelId(),
                citizenId,
                currentYear);

        // ===== BUOC 4: Luu to khai vao bang records =====
        RecordEntity record = RecordEntity.builder()
                .citizenId(citizenId)
                .landParcelId(request.getParcelId())
                .recordCategory("TAX_DECLARATION")
                .currentStatus("SUBMITTED")
                .build();

        RecordEntity savedRecord = recordJpaRepository.save(record);
        log.info("Record created: recordId={}, status=SUBMITTED", savedRecord.getRecordId());

        // ===== BUOC 5: Tao ban ghi tax_payments =====
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
        paymentService.generateTransactionCode(savedPayment);
        log.info("Tax payment created: payId={}, amount={} VND, transactionCode={}", savedPayment.getPayId(),
                savedPayment.getTotalAmountDue(), savedPayment.getTransactionCode());

        // ===== BUOC 6: Xu ly file dinh kem =====
        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            log.info("Linked {} supporting documents with recordId={}", request.getAttachmentIds().size(),
                    savedRecord.getRecordId());
        }

        // Luu ban record TaxDeclaration cho lich su (History)
        BigDecimal actualArea = parcel.getAreaSize();
        BigDecimal declaredArea = request.getDeclaredArea();

        // Phat hien gian lan: chenh lech > 2%
        String status = "SUBMITTED";
        String reviewNote = null;

        if (actualArea != null && declaredArea != null) {
            BigDecimal diff = actualArea.subtract(declaredArea).abs();
            BigDecimal threshold = actualArea.multiply(new BigDecimal("0.02"));
            if (diff.compareTo(threshold) > 0) {
                status = "FRAUD_SUSPECTED";
                reviewNote = "Warning: Declared area (" + declaredArea
                        + ") differs by more than 2% from actual area (" + actualArea + ")";
                log.warn("FRAUD DETECTED for parcel {}: Declared={}, Actual={}", request.getParcelId(), declaredArea,
                        actualArea);
            }
        }

        com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxDeclarationEntity declaration = com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxDeclarationEntity
                .builder()
                .citizenId(citizenId)
                .senderCccd(cccdNumber)
                .parcelId(request.getParcelId())
                .taxYear(currentYear)
                .declaredArea(declaredArea)
                .actualArea(actualArea)
                .declaredPurpose(parcel.getUsageType())
                .status(status)
                .reviewNote(reviewNote)
                .calculatedTaxAmount(taxResult.getTaxAmount())
                .unitPrice(taxResult.getUnitPrice())
                .taxRate(taxResult.getExemptionRate())
                .supportingDocuments(request.getAttachmentIds() != null ? request.getAttachmentIds().toString() : null)
                .build();
        taxDeclarationRepository.save(declaration);

        // Update record status
        if ("FRAUD_SUSPECTED".equals(status)) {
            savedRecord.setCurrentStatus("FRAUD_SUSPECTED");
            recordJpaRepository.save(savedRecord);
        }

        // Ghi log he thong
        auditLogService.log("SUBMIT_DECLARATION", "TAX_DECLARATION",
                String.valueOf(savedRecord.getRecordId()),
                "Citizen submitted tax declaration for parcel " + request.getParcelId()
                        + (reviewNote != null ? " - " + reviewNote : ""));

        // ===== BUOC 7: Tra ve ket qua =====
        return TaxDeclarationResponse.builder()
                .recordId(savedRecord.getRecordId())
                .citizenId(citizenId)
                .parcelId(request.getParcelId())
                .taxYear(currentYear)
                .declaredArea(declaredArea)
                .actualArea(actualArea)
                .declaredPurpose(parcel.getUsageType())
                .status(status)
                .reviewNote(reviewNote)
                .calculatedTaxAmount(taxResult.getTaxAmount())
                .unitPrice(taxResult.getUnitPrice())
                .taxRate(taxResult.getExemptionRate())
                .submittedAt(savedRecord.getSubmittedAt())
                .build();
    }
}
