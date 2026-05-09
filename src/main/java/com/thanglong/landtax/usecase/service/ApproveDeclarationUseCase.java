package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.domain.service.NotificationService;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AccountEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ProcessingLogEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxPaymentEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxBillEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandPriceEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.ProcessingLogJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxPaymentJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxDeclarationRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxBillRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandPriceJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxDeclarationEntity;
import com.thanglong.landtax.usecase.dto.ReviewDeclarationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.math.BigDecimal;

/**
 * Use case xu ly DUYET to khai thue dat.
 *
 * <p><b>Chi cho phep:</b> TAX_OFFICER hoac ADMIN</p>
 *
 * <p><b>Hanh dong:</b></p>
 * <ol>
 *   <li>Kiem tra quyen (role) cua nguoi duyet</li>
 *   <li>Cap nhat records.current_status -> APPROVED</li>
 *   <li>Cap nhat tax_payments.payment_status -> AWAITING_PAYMENT</li>
 *   <li>Ghi nhat ky vao processing_logs</li>
 *   <li>Gui thong bao cho nguoi dan (notifications)</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ApproveDeclarationUseCase {

    private final RecordJpaRepository recordJpaRepository;
    private final TaxPaymentJpaRepository taxPaymentJpaRepository;
    private final ProcessingLogJpaRepository processingLogJpaRepository;
    private final AccountJpaRepository accountJpaRepository;
    private final NotificationService notificationService;
    private final SyncUserFromVneidUseCase syncUserFromVneidUseCase;
    private final AuditLogService auditLogService;
    private final TaxDeclarationRepository taxDeclarationRepository;
    private final TaxBillRepository taxBillRepository;
    private final LandParcelJpaRepository landParcelJpaRepository;
    private final LandPriceJpaRepository landPriceJpaRepository;

    /** Cac role duoc phep duyet to khai */
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "TAX_OFFICER", "ADMIN",
            "ROLE_TAX_OFFICER", "ROLE_ADMIN"
    );

    /**
     * Duyet to khai thue.
     *
     * @param recordId  ID to khai trong bang records
     * @param request   Ghi chu cua can bo duyet (tuy chon)
     * @return Map chua thong tin ket qua duyet
     */
    @Transactional
    public Map<String, Object> approveDeclaration(Integer recordId, ReviewDeclarationRequest request) {

        // ===== BUOC 1: Kiem tra quyen =====
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        validateOfficerRole(auth);

        String cccdNumber = auth.getName();
        Integer officerCitizenId = syncUserFromVneidUseCase.syncAndGetCitizenId(cccdNumber);

        // Lay account_id cua can bo duyet
        AccountEntity officerAccount = accountJpaRepository.findByCitizenId(officerCitizenId)
                .orElseThrow(() -> new RuntimeException(
                        "Officer account not found for citizenId: " + officerCitizenId));

        // ===== BUOC 2: Tim va kiem tra to khai =====
        RecordEntity record = recordJpaRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found: " + recordId));

        String oldStatus = record.getCurrentStatus();

        if (!"VERIFIED".equals(oldStatus)) {
            throw new RuntimeException(
                    "Only records with status VERIFIED can be approved. " +
                            "Current status: " + oldStatus);
        }

        // ===== BUOC 3: Cap nhat records -> APPROVED =====
        record.setCurrentStatus("APPROVED");
        recordJpaRepository.save(record);

        // Tim va cap nhat TaxDeclarationEntity tuong ung
        List<TaxDeclarationEntity> declarations = taxDeclarationRepository
                .findByCitizenIdAndParcelIdAndStatus(record.getCitizenId(), record.getLandParcelId(), "VERIFIED");

        BigDecimal taxAmount = BigDecimal.ZERO;
        if (!declarations.isEmpty()) {
            TaxDeclarationEntity declaration = declarations.get(0);
            declaration.setStatus("APPROVED");
            
            // Tinh toan so tien thue dua tren dien tich dat va bang gia vua duoc cap nhat.
            LandParcelEntity parcel = landParcelJpaRepository.findById(record.getLandParcelId()).orElse(null);
            if (parcel != null && parcel.getLandTypeId() != null && parcel.getAreaId() != null) {
                Optional<LandPriceEntity> priceOpt = landPriceJpaRepository.findLatestPrice(parcel.getLandTypeId(), parcel.getAreaId());
                if (priceOpt.isPresent()) {
                    declaration.setUnitPrice(priceOpt.get().getUnitPrice());
                }
            }

            if (declaration.getActualArea() != null && declaration.getUnitPrice() != null) {
                // Cong thuc chuan: Area * Price * Rate (Gia dinh rate co dinh 0.0003 cho dat o)
                taxAmount = declaration.getActualArea()
                        .multiply(declaration.getUnitPrice())
                        .multiply(new BigDecimal("0.0003"));
                declaration.setCalculatedTaxAmount(taxAmount);
            }
            
            taxDeclarationRepository.save(declaration);
            
            // Sinh Hoa don (Bill)
            TaxBillEntity bill = TaxBillEntity.builder()
                .cccdNumber(declaration.getSenderCccd())
                .amount(taxAmount)
                .status("UNPAID")
                .description("Hoa don thanh toan thue dat cho ho so " + recordId)
                .declarationId(declaration.getId())
                .basePrice(declaration.getUnitPrice())
                .build();
            taxBillRepository.save(bill);
        }

        log.info("Record {} status updated: {} -> APPROVED", recordId, oldStatus);

        // ===== BUOC 4: Cap nhat tax_payments -> AWAITING_PAYMENT =====
        List<TaxPaymentEntity> payments = taxPaymentJpaRepository.findByRecordId(recordId);
        for (TaxPaymentEntity payment : payments) {
            payment.setPaymentStatus("AWAITING_PAYMENT");
            payment.setTotalAmountDue(taxAmount); // dong bo so tien
            taxPaymentJpaRepository.save(payment);
        }

        // ===== BUOC 5: Ghi nhat ky processing_logs =====
        ProcessingLogEntity processingLog = ProcessingLogEntity.builder()
                .recordId(recordId)
                .processorAccountId(officerAccount.getAccountId())
                .processingStep("APPROVE")
                .oldStatus(oldStatus)
                .newStatus("APPROVED")
                .processorNotes(request != null ? request.getProcessorNotes() : null)
                .build();

        processingLogJpaRepository.save(processingLog);

        log.info("Processing log created for record {}: APPROVE by account {}",
                recordId, officerAccount.getAccountId());

        // ===== BUOC 6: Gui thong bao cho nguoi dan =====
        notificationService.notifyDeclarationApproved(record.getCitizenId(), recordId);

        // ===== BUOC 7: Ghi Audit Log =====
        auditLogService.log("APPROVE_DECLARATION", "TAX_DECLARATION", String.valueOf(recordId), "Officer " + cccdNumber + " approved record " + recordId + " and created bill");

        return Map.of(
                "recordId", recordId,
                "oldStatus", oldStatus,
                "newStatus", "APPROVED",
                "paymentStatus", "AWAITING_PAYMENT",
                "approvedBy", cccdNumber,
                "message", "Declaration #" + recordId + " has been approved successfully"
        );
    }

    /**
     * Kiem tra nguoi dung hien tai co quyen duyet hay khong.
     */
    private void validateOfficerRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            throw new RuntimeException("Authentication information missing");
        }

        boolean hasPermission = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ALLOWED_ROLES::contains);

        if (!hasPermission) {
            log.warn("Unauthorized approve attempt by: {}", auth.getName());
            throw new RuntimeException(
                    "You do not have permission to approve declarations. Required roles: TAX_OFFICER or ADMIN");
        }
    }
}
