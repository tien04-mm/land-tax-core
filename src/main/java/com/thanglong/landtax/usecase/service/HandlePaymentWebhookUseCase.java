package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.domain.service.NotificationService;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxPaymentEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxPaymentJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxBillRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.NotificationJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.NotificationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case xu ly webhook thanh toan thanh cong tu PayOS.
 *
 * <p><b>Luong @Transactional:</b></p>
 * <ol>
 *   <li>Tim ban ghi tax_payments bang transaction_code (= orderCode PayOS gui)</li>
 *   <li>Cap nhat payment_status -> PAID, paid_at -> now()</li>
 *   <li>Cap nhat records.current_status -> COMPLETED</li>
 *   <li>Gui thong bao "Cam on ban da nop thue..." cho nguoi dan</li>
 * </ol>
 *
 * <p>Toan bo nam trong @Transactional neu bat ky buoc nao loi, rollback tat ca.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class HandlePaymentWebhookUseCase {

    private final TaxPaymentJpaRepository taxPaymentJpaRepository;
    private final RecordJpaRepository recordJpaRepository;
    private final NotificationService notificationService;
    private final TaxBillRepository taxBillRepository;
    private final NotificationJpaRepository notificationJpaRepository;
    private final AuditLogService auditLogService;

    /**
     * Xu ly thanh toan thanh cong.
     *
     * @param orderCode Ma don hang PayOS gui qua webhook (= transaction_code trong DB)
     */
    @Transactional
    public void handlePaymentSuccess(String orderCode) {
        log.info("Processing PayOS webhook SUCCESS for orderCode: {}", orderCode);

        // ===== BUOC 1: Tim ban ghi thanh toan bang transaction_code =====
        TaxPaymentEntity payment = taxPaymentJpaRepository.findByTransactionCode(orderCode)
                .orElseThrow(() -> {
                    log.error("Payment not found for orderCode: {}", orderCode);
                    return new RuntimeException(
                            "Payment record not found for orderCode: " + orderCode);
                });

        // Kiem tra trang thai de tranh xu ly trung (idempotent)
        if ("PAID".equals(payment.getPaymentStatus())) {
            log.warn("Payment already marked as PAID: payId={}, orderCode={}", payment.getPayId(), orderCode);
            return;
        }

        // ===== BUOC 2: Cap nhat tax_payments -> PAID =====
        payment.setPaymentStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        taxPaymentJpaRepository.save(payment);

        log.info("Payment updated to PAID: payId={}, amount={} VND",
                payment.getPayId(), payment.getTotalAmountDue());

        // Cap nhat TaxBillEntity (neu co)
        taxBillRepository.findById(payment.getPayId()).ifPresent(bill -> {
            bill.setStatus("PAID");
            taxBillRepository.save(bill);
            log.info("TaxBillEntity updated to PAID: billId={}", bill.getBillId());
            
            // Tu dong chen ban ghi thong bao
            NotificationEntity noti = NotificationEntity.builder()
                .accountId(0) // Gia tri dummy neu khong dung account_id
                .notiType("PAYMENT_SUCCESS")
                .cccdNumber(bill.getCccdNumber())
                .message("Bill for declaration " + bill.getDeclarationId() + " has been paid successfully")
                .title("Payment Success")
                .content("Bill for declaration " + bill.getDeclarationId() + " has been paid successfully")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
            notificationJpaRepository.save(noti);
            log.info("Created notification for CCCD: {}", bill.getCccdNumber());
        });

        // ===== BUOC 3: Cap nhat records -> COMPLETED =====
        if (payment.getRecordId() != null) {
            RecordEntity record = recordJpaRepository.findById(payment.getRecordId())
                    .orElse(null);

            if (record != null) {
                String oldStatus = record.getCurrentStatus();
                record.setCurrentStatus("COMPLETED");
                recordJpaRepository.save(record);

                log.info("Record {} status updated: {} -> COMPLETED", record.getRecordId(), oldStatus);

                // ===== BUOC 4: Gui thong bao cho nguoi dan =====
                try {
                    notificationService.notifyPaymentSuccess(
                            record.getCitizenId(),
                            payment.getPayId(),
                            payment.getTotalAmountDue(),
                            payment.getTaxYear()
                    );
                } catch (Exception e) {
                    // Khong rollback transaction neu notification loi
                    log.error("Failed to send payment notification: {}", e.getMessage());
                }
            }
        }
        
        // Ghi AuditLog
        auditLogService.log("WEBHOOK_PAYOS_SUCCESS", "TAX_PAYMENT", orderCode, "System confirmed payment success from PayOS for order " + orderCode);

        log.info("Webhook processing completed for orderCode: {}", orderCode);
    }
}
