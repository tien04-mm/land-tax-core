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
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxDeclarationRepository;
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
import java.util.Set;

/**
 * Use case x  l  T  CH I t  khai thu   t.
 *
 * <p><b>Ch  cho ph p:</b> TAX_OFFICER ho c ADMIN</p>
 * <p><b>B t bu c:</b> processor_notes (l  do t  ch i) kh ng  c   tr ng</p>
 *
 * <p><b>H nh  ng:</b></p>
 * <ol>
 *   <li>Ki m tra quy n (role) c a ng i t  ch i</li>
 *   <li>Validate processor_notes kh ng  c r ng</li>
 *   <li>C p nh t records.current_status   REJECTED</li>
 *   <li>C p nh t tax_payments.payment_status   CANCELLED</li>
 *   <li>Ghi nh t k  v o processing_logs</li>
 *   <li>G i th ng b o cho ng i d n k m l  do t  ch i</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class RejectDeclarationUseCase {

    private final RecordJpaRepository recordJpaRepository;
    private final TaxPaymentJpaRepository taxPaymentJpaRepository;
    private final ProcessingLogJpaRepository processingLogJpaRepository;
    private final AccountJpaRepository accountJpaRepository;
    private final NotificationService notificationService;
    private final SyncUserFromVneidUseCase syncUserFromVneidUseCase;
    private final AuditLogService auditLogService;
    private final TaxDeclarationRepository taxDeclarationRepository;

    /** C c role  c ph p t  ch i t  khai */
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "TAX_OFFICER", "ADMIN",
            "ROLE_TAX_OFFICER", "ROLE_ADMIN"
    );

    /**
     * T  ch i t  khai thu .
     *
     * @param recordId  ID t  khai trong b ng records
     * @param request   L  do t  ch i (processor_notes b t bu c)
     * @return Map ch a th ng tin k t qu  t  ch i
     */
    @Transactional
    public Map<String, Object> rejectDeclaration(Integer recordId, ReviewDeclarationRequest request) {

        // ===== B C 1: Validate l  do t  ch i =====
        if (request == null || request.getProcessorNotes() == null
                || request.getProcessorNotes().isBlank()) {
            throw new RuntimeException(
                    "L  do t  ch i (processor_notes) l  b t bu c khi t  ch i t  khai");
        }

        // ===== B C 2: Ki m tra quy n =====
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        validateOfficerRole(auth);

        String cccdNumber = auth.getName();
        Integer officerCitizenId = syncUserFromVneidUseCase.syncAndGetCitizenId(cccdNumber);

        AccountEntity officerAccount = accountJpaRepository.findByCitizenId(officerCitizenId)
                .orElseThrow(() -> new RuntimeException(
                        "Kh ng t m th y t i kho n c n b  cho citizenId: " + officerCitizenId));

        // ===== B C 3: T m v  ki m tra t  khai =====
        RecordEntity record = recordJpaRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("T  khai kh ng t n t i: " + recordId));

        String oldStatus = record.getCurrentStatus();

        if ("REJECTED".equals(oldStatus) || "APPROVED".equals(oldStatus)) {
            throw new RuntimeException(
                    "Kh ng th  t  ch i t  khai     tr ng th i: " + oldStatus);
        }

        // ===== B C 4: C p nh t records   REJECTED =====
        record.setCurrentStatus("REJECTED");
        recordJpaRepository.save(record);

        // C p nh t TaxDeclarationEntity
        List<TaxDeclarationEntity> declarations = taxDeclarationRepository
                .findByCitizenIdAndParcelIdAndStatus(record.getCitizenId(), record.getLandParcelId(), oldStatus);

        if (!declarations.isEmpty()) {
            TaxDeclarationEntity declaration = declarations.get(0);
            declaration.setStatus("REJECTED");
            declaration.setReviewNote(request.getProcessorNotes());
            taxDeclarationRepository.save(declaration);
            log.info("  c p nh t tr ng th i t  khai {} sang REJECTED", declaration.getId());
        }

        log.info("Record {} status updated: {}   REJECTED", recordId, oldStatus);

        // ===== B C 5: C p nh t tax_payments   CANCELLED =====
        List<TaxPaymentEntity> payments = taxPaymentJpaRepository.findByRecordId(recordId);
        for (TaxPaymentEntity payment : payments) {
            payment.setPaymentStatus("CANCELLED");
            taxPaymentJpaRepository.save(payment);

            log.info("Payment {} status updated   CANCELLED", payment.getPayId());
        }

        // ===== B C 6: Ghi nh t k  processing_logs =====
        ProcessingLogEntity processingLog = ProcessingLogEntity.builder()
                .recordId(recordId)
                .processorAccountId(officerAccount.getAccountId())
                .processingStep("REJECT")
                .oldStatus(oldStatus)
                .newStatus("REJECTED")
                .processorNotes(request.getProcessorNotes())
                .build();

        processingLogJpaRepository.save(processingLog);

        log.info("Processing log created for record {}: REJECT by account {}, reason: '{}'",
                recordId, officerAccount.getAccountId(), request.getProcessorNotes());

        // ===== B C 7: G i th ng b o cho ng i d n k m l  do =====
        notificationService.notifyDeclarationRejected(
                record.getCitizenId(), recordId, request.getProcessorNotes());

        // ===== B C 8: Ghi Audit Log =====
        auditLogService.log("REJECT_DECLARATION", "TAX_DECLARATION", String.valueOf(recordId), "C n b  thu  " + cccdNumber + "   t  ch i h  s  " + recordId + ". L  do: " + request.getProcessorNotes());

        return Map.of(
                "recordId", recordId,
                "oldStatus", oldStatus,
                "newStatus", "REJECTED",
                "reason", request.getProcessorNotes(),
                "rejectedBy", cccdNumber,
                "message", "T  khai m  #" + recordId + "   b  t  ch i"
        );
    }

    /**
     * Ki m tra ng i d ng hi n t i c  quy n t  ch i hay kh ng.
     */
    private void validateOfficerRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            throw new RuntimeException("Kh ng c  th ng tin x c th c");
        }

        boolean hasPermission = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ALLOWED_ROLES::contains);

        if (!hasPermission) {
            log.warn("Unauthorized reject attempt by: {}", auth.getName());
            throw new RuntimeException(
                    "B n kh ng c  quy n t  ch i t  khai. Y u c u vai tr : TAX_OFFICER ho c ADMIN");
        }
    }
}

