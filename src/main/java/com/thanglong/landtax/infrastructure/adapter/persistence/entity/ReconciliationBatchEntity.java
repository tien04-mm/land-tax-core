package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Integer batchId;

    @Column(name = "officer_account_id", nullable = false)
    private Integer officerAccountId;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "matched_count")
    private Integer matchedCount;

    @Column(name = "error_count")
    private Integer errorCount;

    @Column(name = "batch_notes", columnDefinition = "TEXT")
    private String batchNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
