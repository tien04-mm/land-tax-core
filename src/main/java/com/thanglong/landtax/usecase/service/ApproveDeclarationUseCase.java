package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.domain.service.NotificationService;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AccountEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ProcessingLogEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxPaymentEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.ProcessingLogJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxPaymentJpaRepository;
import com.thanglong.landtax.usecase.dto.ReviewDeclarationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Use case xu ly DUYET to khai thue dat.
 *
 * <p>
 * <b>Chi cho phep:</b> TAX_OFFICER hoac ADMIN
 * </p>
 *
 * <p>
 * <b>Hanh dong:</b>
 * </p>
 * <ol>
 * <li>Kiem tra quyen (role) cua nguoi duyet</li>
 * <li>Cap nhat records.current_status -> APPROVED</li>
 * <li>Cap nhat tax_payments.payment_status -> AWAITING_PAYMENT</li>
 * <li>Ghi nhat ky vao processing_logs</li>
 * <li>Gui thong bao cho nguoi dan (notifications)</li>
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

    /**
     * Duyet to khai thue.
     *
     * @param recordId ID to khai trong bang records
     * @param request  Ghi chu cua can bo duyet (tuy chon)
     * @return Map chua thong tin ket qua duyet
     */
    @Transactional
    public Map<String, Object> approveDeclaration(Integer recordId, ReviewDeclarationRequest request) {

        // ===== BUOC 1: Kiem tra quyen =====
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

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

        // ===== BUOC 4: Cap nhat tax_payments -> AWAITING_PAYMENT =====
        List<TaxPaymentEntity> payments = taxPaymentJpaRepository.findByRecordId(recordId);
        for (TaxPaymentEntity payment : payments) {
            payment.setPaymentStatus("AWAITING_PAYMENT");
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
        auditLogService.log("APPROVE_DECLARATION", "TAX_DECLARATION", String.valueOf(recordId),
                "Officer " + cccdNumber + " approved record " + recordId + " and created bill");

        return Map.of(
                "recordId", recordId,
                "oldStatus", oldStatus,
                "newStatus", "APPROVED",
                "paymentStatus", "AWAITING_PAYMENT",
                "approvedBy", cccdNumber,
                "message", "Declaration #" + recordId + " has been approved successfully");
    }

}
