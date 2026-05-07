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
import java.util.Optional;
import java.util.Set;
import java.math.BigDecimal;

/**
 * Use case xử lý DUYỆT tờ khai thuế đất.
 *
 * <p><b>Chỉ cho phép:</b> TAX_OFFICER hoặc ADMIN</p>
 *
 * <p><b>Hành động:</b></p>
 * <ol>
 *   <li>Kiểm tra quyền (role) của người duyệt</li>
 *   <li>Cập nhật records.current_status → APPROVED</li>
 *   <li>Cập nhật tax_payments.payment_status → AWAITING_PAYMENT</li>
 *   <li>Ghi nhật ký vào processing_logs</li>
 *   <li>Gửi thông báo cho người dân (notifications)</li>
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

    /** Các role được phép duyệt tờ khai */
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "TAX_OFFICER", "ADMIN",
            "ROLE_TAX_OFFICER", "ROLE_ADMIN"
    );

    /**
     * Duyệt tờ khai thuế.
     *
     * @param recordId  ID tờ khai trong bảng records
     * @param request   Ghi chú của cán bộ duyệt (tùy chọn)
     * @return Map chứa thông tin kết quả duyệt
     */
    @Transactional
    public Map<String, Object> approveDeclaration(Integer recordId, ReviewDeclarationRequest request) {

        // ===== BƯỚC 1: Kiểm tra quyền =====
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        validateOfficerRole(auth);

        String cccdNumber = auth.getName();
        Integer officerCitizenId = syncUserFromVneidUseCase.syncAndGetCitizenId(cccdNumber);

        // Lấy account_id của cán bộ duyệt
        AccountEntity officerAccount = accountJpaRepository.findByCitizenId(officerCitizenId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy tài khoản cán bộ cho citizenId: " + officerCitizenId));

        // ===== BƯỚC 2: Tìm và kiểm tra tờ khai =====
        RecordEntity record = recordJpaRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Tờ khai không tồn tại: " + recordId));

        String oldStatus = record.getCurrentStatus();

        if (!"PENDING".equals(oldStatus) && !"WARNING_FRAUD".equals(oldStatus)) {
            throw new RuntimeException(
                    "Chỉ có thể duyệt tờ khai ở trạng thái PENDING hoặc WARNING_FRAUD. " +
                            "Trạng thái hiện tại: " + oldStatus);
        }

        // ===== BƯỚC 3: Cập nhật records → APPROVED =====
        record.setCurrentStatus("APPROVED");
        recordJpaRepository.save(record);

        // Tìm và cập nhật TaxDeclarationEntity tương ứng
        log.info("Searching for TaxDeclaration: citizenId={}, parcelId={}, status=PENDING", 
                record.getCitizenId(), record.getLandParcelId());
                
        List<TaxDeclarationEntity> declarations = taxDeclarationRepository
                .findByCitizenIdAndParcelIdAndStatus(record.getCitizenId(), record.getLandParcelId(), "PENDING");

        if (!declarations.isEmpty()) {
            TaxDeclarationEntity declaration = declarations.get(0);
            declaration.setStatus("APPROVED");
            
            // Tính toán: calculatedTaxAmount = actualArea * unitPrice * 0.0003 * 0.5
            if (declaration.getActualArea() != null && declaration.getUnitPrice() != null) {
                BigDecimal taxAmount = declaration.getActualArea()
                        .multiply(declaration.getUnitPrice())
                        .multiply(new BigDecimal("0.0003"))
                        .multiply(new BigDecimal("0.5"));
                declaration.setCalculatedTaxAmount(taxAmount);
                log.info("Updating TaxDeclaration {}: status=APPROVED, calculatedTaxAmount={}", 
                        declaration.getId(), taxAmount);
            } else {
                log.warn("TaxDeclaration {} has null area or unit price, skipping calculation", declaration.getId());
            }
            
            taxDeclarationRepository.save(declaration);
            log.info("TaxDeclaration {} saved successfully", declaration.getId());
        } else {
            log.warn("No PENDING TaxDeclaration found for citizenId={} and parcelId={}", 
                    record.getCitizenId(), record.getLandParcelId());
        }

        log.info("Record {} status updated: {} → APPROVED", recordId, oldStatus);

        // ===== BƯỚC 4: Cập nhật tax_payments → AWAITING_PAYMENT =====
        List<TaxPaymentEntity> payments = taxPaymentJpaRepository.findByRecordId(recordId);
        for (TaxPaymentEntity payment : payments) {
            payment.setPaymentStatus("AWAITING_PAYMENT");
            taxPaymentJpaRepository.save(payment);

            log.info("Payment {} status updated: UNPAID → AWAITING_PAYMENT", payment.getPayId());
        }

        // ===== BƯỚC 5: Ghi nhật ký processing_logs =====
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

        // ===== BƯỚC 6: Gửi thông báo cho người dân =====
        notificationService.notifyDeclarationApproved(record.getCitizenId(), recordId);

        // ===== BƯỚC 7: Ghi Audit Log =====
        auditLogService.log("APPROVE_DECLARATION", "TAX_DECLARATION", String.valueOf(recordId), "Duyệt tờ khai thuế");

        return Map.of(
                "recordId", recordId,
                "oldStatus", oldStatus,
                "newStatus", "APPROVED",
                "paymentStatus", "AWAITING_PAYMENT",
                "approvedBy", cccdNumber,
                "message", "Tờ khai mã #" + recordId + " đã được duyệt thành công"
        );
    }

    /**
     * Kiểm tra người dùng hiện tại có quyền duyệt hay không.
     */
    private void validateOfficerRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            throw new RuntimeException("Không có thông tin xác thực");
        }

        boolean hasPermission = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ALLOWED_ROLES::contains);

        if (!hasPermission) {
            log.warn("Unauthorized approve attempt by: {}", auth.getName());
            throw new RuntimeException(
                    "Bạn không có quyền duyệt tờ khai. Yêu cầu vai trò: TAX_OFFICER hoặc ADMIN");
        }
    }
}
