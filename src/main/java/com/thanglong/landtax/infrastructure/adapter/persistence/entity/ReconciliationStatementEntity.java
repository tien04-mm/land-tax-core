package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity lưu từng dòng trong file sao kê ngân hàng đã upload.
 * Sau khi so khớp với tax_bills, trường matchStatus sẽ là MATCHED hoặc DISCREPANCY.
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

    /** Mã giao dịch ngân hàng (orderCode / transaction reference) */
    @Column(name = "bank_transaction_ref", length = 100)
    private String bankTransactionRef;

    /** Số tiền trong sao kê ngân hàng */
    @Column(name = "bank_amount", precision = 18, scale = 2)
    private BigDecimal bankAmount;

    /** Ngày giao dịch trong sao kê */
    @Column(name = "bank_transaction_date")
    private LocalDateTime bankTransactionDate;

    /** Nội dung chuyển khoản */
    @Column(name = "bank_description", columnDefinition = "TEXT")
    private String bankDescription;

    /** bill_id trong tax_bills nếu tìm thấy khớp */
    @Column(name = "matched_bill_id")
    private Integer matchedBillId;

    /** MATCHED | DISCREPANCY | UNMATCHED */
    @Column(name = "match_status", length = 30)
    private String matchStatus;

    /** Ghi chú sai lệch (nếu DISCREPANCY) */
    @Column(name = "discrepancy_note", columnDefinition = "TEXT")
    private String discrepancyNote;

    /** Tên file sao kê gốc */
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
