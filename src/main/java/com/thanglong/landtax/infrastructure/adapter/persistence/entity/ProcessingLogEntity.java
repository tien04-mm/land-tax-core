package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA Entity mapping chính xác với bảng processing_logs trong land_tax_management.sql.
 * Lưu nhật ký xử lý hồ sơ: ai duyệt, bước nào, trạng thái cũ/mới, ghi chú.
 */
@Entity
@Table(name = "processing_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plog_id")
    private Integer plogId;

    @Column(name = "record_id", nullable = false)
    private Integer recordId;                   // FK → records.record_id

    @Column(name = "processor_account_id", nullable = false)
    private Integer processorAccountId;         // FK → accounts.account_id (người duyệt)

    @Column(name = "processing_step", nullable = false, length = 100)
    private String processingStep;              // APPROVE, REJECT, REVIEW, ...

    @Column(name = "old_status", length = 50)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 50)
    private String newStatus;

    @Column(name = "processor_notes", columnDefinition = "TEXT")
    private String processorNotes;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }
}
