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

    @Column(name = "amount_received", precision = 18, scale = 2, nullable = false)
    private BigDecimal amountReceived;

    @Column(name = "bank_trans_id", length = 100)
    private String bankTransId;

    @Column(name = "webhook_payload", columnDefinition = "LONGTEXT")
    private String webhookPayload;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
