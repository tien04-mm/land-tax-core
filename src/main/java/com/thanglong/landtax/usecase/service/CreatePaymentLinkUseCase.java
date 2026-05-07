package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.external.PayOSAdapter;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxPaymentEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxPaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Use case tạo link thanh toán trực tuyến qua PayOS.
 *
 * <p><b>Luồng xử lý:</b></p>
 * <ol>
 *   <li>Tìm bản ghi thanh toán (tax_payments) theo pay_id</li>
 *   <li>Kiểm tra payment_status = AWAITING_PAYMENT</li>
 *   <li>Sinh order_code unique cho PayOS (dùng timestamp + pay_id)</li>
 *   <li>Gọi PayOSAdapter.createPaymentLink() với total_amount_due</li>
 *   <li>Lưu order_code vào transaction_code để đối soát webhook</li>
 *   <li>Trả về checkout URL cho frontend redirect</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class CreatePaymentLinkUseCase {

    private final TaxPaymentJpaRepository taxPaymentJpaRepository;
    private final PayOSAdapter payOSAdapter;

    @Value("${payos.return-url:http://localhost:3000/payment/success}")
    private String returnUrl;

    @Value("${payos.cancel-url:http://localhost:3000/payment/cancel}")
    private String cancelUrl;

    /**
     * Tạo link thanh toán cho một khoản thuế.
     *
     * @param payId ID bản ghi trong bảng tax_payments
     * @return Map chứa checkoutUrl, orderCode, amount, qrCode
     */
    @Transactional
    public Map<String, Object> createPaymentLink(Integer payId) {

        // ===== BƯỚC 1: Tìm bản ghi thanh toán =====
        TaxPaymentEntity payment = taxPaymentJpaRepository.findById(payId)
                .orElseThrow(() -> new RuntimeException("Bản ghi thanh toán không tồn tại: " + payId));

        // ===== BƯỚC 2: Kiểm tra trạng thái =====
        if (!"AWAITING_PAYMENT".equals(payment.getPaymentStatus())) {
            throw new RuntimeException(
                    String.format("Không thể tạo link thanh toán. Trạng thái hiện tại: %s. " +
                            "Chỉ cho phép khi trạng thái là AWAITING_PAYMENT", payment.getPaymentStatus()));
        }

        // ===== BƯỚC 3: Sinh order_code unique cho PayOS =====
        // Format: payId * 1000 + timestamp_last_3_digits → đảm bảo unique
        long orderCode = (long) payId * 10000 + (System.currentTimeMillis() % 10000);

        // ===== BƯỚC 4: Tính số tiền (PayOS yêu cầu VNĐ, số nguyên) =====
        long amount = payment.getTotalAmountDue().longValue();
        if (payment.getLateFeeAmount() != null) {
            amount += payment.getLateFeeAmount().longValue();
        }

        String description = String.format("Nộp thuế đất năm %d - Mã TT #%d",
                payment.getTaxYear(), payId);

        log.info("Creating payment link: payId={}, orderCode={}, amount={} VNĐ",
                payId, orderCode, amount);

        // ===== BƯỚC 5: Gọi PayOS API =====
        PayOSAdapter.PaymentLinkResult result = payOSAdapter.createPaymentLink(
                orderCode, amount, description, returnUrl, cancelUrl);

        // ===== BƯỚC 6: Lưu order_code vào transaction_code =====
        payment.setTransactionCode(String.valueOf(orderCode));
        taxPaymentJpaRepository.save(payment);

        log.info("Payment link created and transaction_code saved: payId={}, orderCode={}",
                payId, orderCode);

        return Map.of(
                "payId", payId,
                "orderCode", result.getOrderCode(),
                "amount", result.getAmount(),
                "checkoutUrl", result.getCheckoutUrl(),
                "qrCode", result.getQrCode(),
                "description", result.getDescription(),
                "message", "Link thanh toán đã được tạo thành công"
        );
    }
}
