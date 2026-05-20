package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ReconciliationBatchEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ReconciliationLogEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxPaymentEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TaxPaymentJpaRepository taxPaymentJpaRepository;
    private final ReconciliationLogJpaRepository reconciliationLogJpaRepository;
    private final ReconciliationBatchJpaRepository reconciliationBatchJpaRepository;
    private final RecordJpaRepository recordJpaRepository;

    /**
     * Sinh ma transactionCode ngau nhien va duy nhat khi hoa don/khoan thanh toan duoc tao.
     */
    public String generateTransactionCode(TaxPaymentEntity payment) {
        String code = "TX-" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12).toUpperCase();
        payment.setTransactionCode(code);
        taxPaymentJpaRepository.save(payment);
        log.info("Generated transactionCode: {} for payId: {}", code, payment.getPayId());
        return code;
    }

    /**
     * Xu ly webhook ngan hang tu dong.
     */
    @Transactional
    public void processBankTransferWebhook(String transactionCode, BigDecimal bankAmount, String bankName) {
        log.info("Processing bank transfer webhook: code={}, amount={}, bank={}", transactionCode, bankAmount, bankName);

        // Tim ban ghi thanh toan
        TaxPaymentEntity payment = taxPaymentJpaRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new RuntimeException("Payment not found for transactionCode: " + transactionCode));

        // Tao reconciliation batch
        String batchCode = "BATCH-" + java.time.LocalDate.now().toString() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ReconciliationBatchEntity batch = ReconciliationBatchEntity.builder()
                .batchCode(batchCode)
                .status("COMPLETED")
                .build();
        reconciliationBatchJpaRepository.save(batch);

        BigDecimal systemAmount = payment.getTotalAmountDue();
        String matchStatus;

        if (bankAmount.compareTo(systemAmount) == 0) {
            matchStatus = "MATCHED";

            // Cap nhat trang thai thanh toan
            payment.setPaymentStatus("PAID");
            payment.setPaidAt(LocalDateTime.now());
            taxPaymentJpaRepository.save(payment);

            // Cap nhat record sang COMPLETED
            if (payment.getRecordId() != null) {
                recordJpaRepository.findById(payment.getRecordId()).ifPresent(record -> {
                    record.setCurrentStatus("COMPLETED");
                    recordJpaRepository.save(record);
                    log.info("Record {} status updated to COMPLETED", record.getRecordId());
                });
            }


        } else {
            matchStatus = "DISCREPANCY";
            payment.setPaymentStatus("DISCREPANCY");
            taxPaymentJpaRepository.save(payment);
            log.warn("DISCREPANCY: bankAmount={}, systemAmount={}", bankAmount, systemAmount);
        }

        // Luu reconciliation log
        ReconciliationLogEntity logEntity = ReconciliationLogEntity.builder()
                .transactionCode(transactionCode)
                .bankAmount(bankAmount)
                .systemAmount(systemAmount)
                .matchStatus(matchStatus)
                .taxPaymentId(payment.getPayId())
                .bankName(bankName)
                .build();
        reconciliationLogJpaRepository.save(logEntity);
    }
}
