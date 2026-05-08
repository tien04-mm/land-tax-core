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
 * Use case xử lý webhook thanh toán thành công từ PayOS.
 *
 * <p><b>Luồng @Transactional:</b></p>
 * <ol>
 *   <li>Tìm bản ghi tax_payments bằng transaction_code (= orderCode PayOS gửi)</li>
 *   <li>Cập nhật payment_status → PAID, paid_at → now()</li>
 *   <li>Cập nhật records.current_status → COMPLETED</li>
 *   <li>Gửi thông báo "Cảm ơn bạn đã nộp thuế..." cho người dân</li>
 * </ol>
 *
 * <p>Toàn bộ nằm trong @Transactional → nếu bất kỳ bước nào lỗi, rollback tất cả.</p>
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
     * Xử lý thanh toán thành công.
     *
     * @param orderCode Mã đơn hàng PayOS gửi qua webhook (= transaction_code trong DB)
     */
    @Transactional
    public void handlePaymentSuccess(String orderCode) {
        log.info("Processing PayOS webhook SUCCESS for orderCode: {}", orderCode);

        // ===== BƯỚC 1: Tìm bản ghi thanh toán bằng transaction_code =====
        TaxPaymentEntity payment = taxPaymentJpaRepository.findByTransactionCode(orderCode)
                .orElseThrow(() -> {
                    log.error("Payment not found for orderCode: {}", orderCode);
                    return new RuntimeException(
                            "Không tìm thấy bản ghi thanh toán cho orderCode: " + orderCode);
                });

        // Kiểm tra trạng thái — tránh xử lý trùng (idempotent)
        if ("PAID".equals(payment.getPaymentStatus())) {
            log.warn("Payment already marked as PAID: payId={}, orderCode={}", payment.getPayId(), orderCode);
            return;
        }

        // ===== BƯỚC 2: Cập nhật tax_payments → PAID =====
        payment.setPaymentStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        taxPaymentJpaRepository.save(payment);

        log.info("Payment updated to PAID: payId={}, amount={} VNĐ",
                payment.getPayId(), payment.getTotalAmountDue());

        // Cập nhật TaxBillEntity (nếu có)
        taxBillRepository.findById(payment.getPayId()).ifPresent(bill -> {
            bill.setStatus("PAID");
            taxBillRepository.save(bill);
            log.info("TaxBillEntity updated to PAID: billId={}", bill.getBillId());
            
            // Tự động chèn bản ghi thông báo
            NotificationEntity noti = NotificationEntity.builder()
                .accountId(0) // Giá trị dummy nếu không dùng account_id
                .notiType("PAYMENT_SUCCESS")
                .cccdNumber(bill.getCccdNumber())
                .message("Hóa đơn cho hồ sơ " + bill.getDeclarationId() + " đã được thanh toán thành công")
                .title("Thanh toán thành công")
                .content("Hóa đơn cho hồ sơ " + bill.getDeclarationId() + " đã được thanh toán thành công")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
            notificationJpaRepository.save(noti);
            log.info("Đã tạo thông báo hộp thư cho CCCD: {}", bill.getCccdNumber());
        });

        // ===== BƯỚC 3: Cập nhật records → COMPLETED =====
        if (payment.getRecordId() != null) {
            RecordEntity record = recordJpaRepository.findById(payment.getRecordId())
                    .orElse(null);

            if (record != null) {
                String oldStatus = record.getCurrentStatus();
                record.setCurrentStatus("COMPLETED");
                recordJpaRepository.save(record);

                log.info("Record {} status updated: {} → COMPLETED", record.getRecordId(), oldStatus);

                // ===== BƯỚC 4: Gửi thông báo cho người dân =====
                try {
                    notificationService.notifyPaymentSuccess(
                            record.getCitizenId(),
                            payment.getPayId(),
                            payment.getTotalAmountDue(),
                            payment.getTaxYear()
                    );
                } catch (Exception e) {
                    // Không rollback transaction nếu notification lỗi
                    log.error("Failed to send payment notification: {}", e.getMessage());
                }
            }
        }
        
        // Ghi AuditLog
        auditLogService.log("WEBHOOK_PAYOS_SUCCESS", "TAX_PAYMENT", orderCode, "Hệ thống xác nhận thanh toán thành công từ PayOS cho đơn hàng " + orderCode);

        log.info("Webhook processing completed for orderCode: {}", orderCode);
    }
}
