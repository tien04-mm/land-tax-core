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
 * Use case xử lý TỪ CHỐI tờ khai thuế đất.
 *
 * <p><b>Chỉ cho phép:</b> TAX_OFFICER hoặc ADMIN</p>
 * <p><b>Bắt buộc:</b> processor_notes (lý do từ chối) không được để trống</p>
 *
 * <p><b>Hành động:</b></p>
 * <ol>
 *   <li>Kiểm tra quyền (role) của người từ chối</li>
 *   <li>Validate processor_notes không được rỗng</li>
 *   <li>Cập nhật records.current_status → REJECTED</li>
 *   <li>Cập nhật tax_payments.payment_status → CANCELLED</li>
 *   <li>Ghi nhật ký vào processing_logs</li>
 *   <li>Gửi thông báo cho người dân kèm lý do từ chối</li>
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

    /** Các role được phép từ chối tờ khai */
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "TAX_OFFICER", "ADMIN",
            "ROLE_TAX_OFFICER", "ROLE_ADMIN"
    );

    /**
     * Từ chối tờ khai thuế.
     *
     * @param recordId  ID tờ khai trong bảng records
     * @param request   Lý do từ chối (processor_notes bắt buộc)
     * @return Map chứa thông tin kết quả từ chối
     */
    @Transactional
    public Map<String, Object> rejectDeclaration(Integer recordId, ReviewDeclarationRequest request) {

        // ===== BƯỚC 1: Validate lý do từ chối =====
        if (request == null || request.getProcessorNotes() == null
                || request.getProcessorNotes().isBlank()) {
            throw new RuntimeException(
                    "Lý do từ chối (processor_notes) là bắt buộc khi từ chối tờ khai");
        }

        // ===== BƯỚC 2: Kiểm tra quyền =====
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        validateOfficerRole(auth);

        String cccdNumber = auth.getName();
        Integer officerCitizenId = syncUserFromVneidUseCase.syncAndGetCitizenId(cccdNumber);

        AccountEntity officerAccount = accountJpaRepository.findByCitizenId(officerCitizenId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy tài khoản cán bộ cho citizenId: " + officerCitizenId));

        // ===== BƯỚC 3: Tìm và kiểm tra tờ khai =====
        RecordEntity record = recordJpaRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Tờ khai không tồn tại: " + recordId));

        String oldStatus = record.getCurrentStatus();

        if ("REJECTED".equals(oldStatus) || "APPROVED".equals(oldStatus)) {
            throw new RuntimeException(
                    "Không thể từ chối tờ khai đã ở trạng thái: " + oldStatus);
        }

        // ===== BƯỚC 4: Cập nhật records → REJECTED =====
        record.setCurrentStatus("REJECTED");
        recordJpaRepository.save(record);

        // Cập nhật TaxDeclarationEntity
        List<TaxDeclarationEntity> declarations = taxDeclarationRepository
                .findByCitizenIdAndParcelIdAndStatus(record.getCitizenId(), record.getLandParcelId(), oldStatus);

        if (!declarations.isEmpty()) {
            TaxDeclarationEntity declaration = declarations.get(0);
            declaration.setStatus("REJECTED");
            declaration.setReviewNote(request.getProcessorNotes());
            taxDeclarationRepository.save(declaration);
            log.info("Đã cập nhật trạng thái tờ khai {} sang REJECTED", declaration.getId());
        }

        log.info("Record {} status updated: {} → REJECTED", recordId, oldStatus);

        // ===== BƯỚC 5: Cập nhật tax_payments → CANCELLED =====
        List<TaxPaymentEntity> payments = taxPaymentJpaRepository.findByRecordId(recordId);
        for (TaxPaymentEntity payment : payments) {
            payment.setPaymentStatus("CANCELLED");
            taxPaymentJpaRepository.save(payment);

            log.info("Payment {} status updated → CANCELLED", payment.getPayId());
        }

        // ===== BƯỚC 6: Ghi nhật ký processing_logs =====
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

        // ===== BƯỚC 7: Gửi thông báo cho người dân kèm lý do =====
        notificationService.notifyDeclarationRejected(
                record.getCitizenId(), recordId, request.getProcessorNotes());

        // ===== BƯỚC 8: Ghi Audit Log =====
        auditLogService.log("REJECT_DECLARATION", "TAX_DECLARATION", String.valueOf(recordId), "Cán bộ thuế " + cccdNumber + " đã từ chối hồ sơ " + recordId + ". Lý do: " + request.getProcessorNotes());

        return Map.of(
                "recordId", recordId,
                "oldStatus", oldStatus,
                "newStatus", "REJECTED",
                "reason", request.getProcessorNotes(),
                "rejectedBy", cccdNumber,
                "message", "Tờ khai mã #" + recordId + " đã bị từ chối"
        );
    }

    /**
     * Kiểm tra người dùng hiện tại có quyền từ chối hay không.
     */
    private void validateOfficerRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            throw new RuntimeException("Không có thông tin xác thực");
        }

        boolean hasPermission = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ALLOWED_ROLES::contains);

        if (!hasPermission) {
            log.warn("Unauthorized reject attempt by: {}", auth.getName());
            throw new RuntimeException(
                    "Bạn không có quyền từ chối tờ khai. Yêu cầu vai trò: TAX_OFFICER hoặc ADMIN");
        }
    }
}
