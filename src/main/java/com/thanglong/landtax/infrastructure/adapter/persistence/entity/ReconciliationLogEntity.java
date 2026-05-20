package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity luu vet doi soat tu dong khi nhan webhook tu ngan hang.
 * Moi ban ghi tuong ung mot giao dich ngan hang da duoc so khop (hoac sai lech).
 */
@Entity
@Table(name = "reconciliation_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;

    /** Ma giao dich (transaction_code) khop voi tax_payments */
    @Column(name = "transaction_code", length = 100, nullable = false)
    private String transactionCode;

    /** So tien ngan hang gui qua webhook */
    @Column(name = "bank_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal bankAmount;

    /** So tien he thong ghi nhan trong tax_payments */
    @Column(name = "system_amount", precision = 18, scale = 2)
    private BigDecimal systemAmount;

    /** MATCHED | DISCREPANCY */
    @Column(name = "match_status", length = 30, nullable = false)
    private String matchStatus;

    /** FK tro ve tax_payments.pay_id */
    @Column(name = "tax_payment_id")
    private Integer taxPaymentId;

    /** Ten ngan hang gui webhook */
    @Column(name = "bank_name", length = 100)
    private String bankName;

    /** Thoi diem he thong nhan va xu ly webhook */
    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @PrePersist
    protected void onCreate() {
        if (matchedAt == null) {
            matchedAt = LocalDateTime.now();
        }
    }
}
