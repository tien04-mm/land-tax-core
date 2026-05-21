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
 * Use case tao link thanh toan truc tuyen qua PayOS.
 *
 * <p><b>Luong xu ly:</b></p>
 * <ol>
 *   <li>Tim ban ghi thanh toan (tax_payments) theo pay_id</li>
 *   <li>Kiem tra payment_status = AWAITING_PAYMENT</li>
 *   <li>Sinh order_code unique cho PayOS (dung timestamp + pay_id)</li>
 *   <li>Goi PayOSAdapter.createPaymentLink() voi total_amount_due</li>
 *   <li>Luu order_code vao transaction_code de doi soat webhook</li>
 *   <li>Tra ve checkout URL cho frontend redirect</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class CreatePaymentLinkUseCase {

    private final TaxPaymentJpaRepository taxPaymentJpaRepository;
    private final PayOSAdapter payOSAdapter;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository recordJpaRepository;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository citizenLocalJpaRepository;

    @Value("${payos.return-url:http://localhost:3000/payment/success}")
    private String returnUrl;

    @Value("${payos.cancel-url:http://localhost:3000/payment/cancel}")
    private String cancelUrl;

    /**
     * Tao link thanh toan cho mot khoan thue.
     *
     * @param payId ID ban ghi trong bang tax_payments
     * @return Map chua checkoutUrl, orderCode, amount, qrCode
     */
    @Transactional
    public Map<String, Object> createPaymentLink(Integer payId) {

        // ===== BUOC 1: Tim ban ghi thanh toan =====
        TaxPaymentEntity payment = taxPaymentJpaRepository.findById(payId)
                .orElseThrow(() -> new RuntimeException("Payment record not found: " + payId));

        // ===== BUOC 2: Kiem tra trang thai =====
        if (!"AWAITING_PAYMENT".equals(payment.getPaymentStatus())) {
            throw new RuntimeException(
                    String.format("Cannot create payment link. Current status: %s. " +
                            "Only allowed when status is AWAITING_PAYMENT", payment.getPaymentStatus()));
        }

        // ===== BUOC 2.5: Kiem tra quyen so huu =====
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String currentUsername = authentication.getName();
            boolean isAdminOrOfficer = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_TAX_OFFICER") || a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdminOrOfficer) {
                if (payment.getRecordId() == null) {
                    throw new RuntimeException("Payment is not linked to any record, cannot verify permissions.");
                }

                // Check ownership via record -> citizen
                com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity record = recordJpaRepository.findById(payment.getRecordId())
                        .orElseThrow(() -> new RuntimeException("Linked record not found."));
                com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity citizen = citizenLocalJpaRepository.findById(record.getCitizenId())
                        .orElseThrow(() -> new RuntimeException("Citizen info not found."));
                
                if (!citizen.getCccdNumber().equals(currentUsername)) {
                    throw new org.springframework.security.access.AccessDeniedException("You do not have permission to create a payment link for this tax amount.");
                }
            }
        }

        // ===== BUOC 3: Sinh order_code unique cho PayOS =====
        long orderCode = (long) payId * 10000 + (System.currentTimeMillis() % 10000);

        // ===== BUOC 4: Tinh so tien =====
        long amount = payment.getTotalAmountDue().longValue();

        // Toi uu description cho PayOS (gioi han 25 ky tu). VD: "Thue 2026 #777" (chi 14 ky tu)
        String description = String.format("Thue %d #%d", payment.getTaxYear(), payId);

        // GOI PAYOS (Luu y ep kieu amount va orderCode ve int neu ham yeu cau)
        PayOSAdapter.PaymentLinkResult result = payOSAdapter.createPaymentLink(
                (int) orderCode, 
                (int) amount, 
                description, 
                returnUrl, 
                cancelUrl
        );

        // ===== BUOC 6: Luu order_code vao transaction_code =====
        payment.setTransactionCode(String.valueOf(orderCode));
        taxPaymentJpaRepository.save(payment);

        log.info("Payment link created and transaction_code saved: payId={}, orderCode={}",
                payId, orderCode);

        return Map.of(
                "success", true,
                "payId", payId,
                "orderCode", result.getOrderCode(),
                "amount", result.getAmount(),
                "checkoutUrl", result.getCheckoutUrl(),
                "qrCode", result.getQrCode(),
                "description", result.getDescription(),
                "message", "Payment link created successfully"
        );
    }
}
