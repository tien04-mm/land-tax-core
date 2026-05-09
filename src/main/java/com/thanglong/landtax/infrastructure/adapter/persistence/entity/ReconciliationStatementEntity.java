package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity luu tung dong trong file sao ke ngan hang da upload.
 * Sau khi so khop voi tax_bills, truong matchStatus se la MATCHED hoac DISCREPANCY.
 */
@Entity
@Table(name = "reconciliation_statements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationStatementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Ma giao dich ngan hang (orderCode / transaction reference) */
    @Column(name = "bank_transaction_ref", length = 100)
    private String bankTransactionRef;

    /** So tien trong sao ke ngan hang */
    @Column(name = "bank_amount", precision = 18, scale = 2)
    private BigDecimal bankAmount;

    /** Ngay giao dich trong sao ke */
    @Column(name = "bank_transaction_date")
    private LocalDateTime bankTransactionDate;

    /** Noi dung chuyen khoan */
    @Column(name = "bank_description", columnDefinition = "TEXT")
    private String bankDescription;

    /** bill_id trong tax_bills neu tim thay khop */
    @Column(name = "matched_bill_id")
    private Integer matchedBillId;

    /** MATCHED | DISCREPANCY | UNMATCHED */
    @Column(name = "match_status", length = 30)
    private String matchStatus;

    /** Ghi chu sai lech (neu DISCREPANCY) */
    @Column(name = "discrepancy_note", columnDefinition = "TEXT")
    private String discrepancyNote;

    /** Ten file sao ke goc */
    @Column(name = "source_file", length = 255)
    private String sourceFile;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) uploadedAt = LocalDateTime.now();
        if (matchStatus == null) matchStatus = "UNMATCHED";
    }
}
