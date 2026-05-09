package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mutation_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MutationRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mutation_id")
    private Long mutationId;

    @Column(name = "parcel_id")
    private Integer parcelId;

    @Column(name = "mutation_type", length = 30)
    private String mutationType;

    @Column(name = "new_owner_cccd", length = 12)
    private String newOwnerCccd;

    @Column(name = "description")
    private String description;

    @Column(name = "fee_amount", precision = 15, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "status", length = 30)
    private String status; // PENDING, APPROVED, REJECTED, NEED_MORE_DOCS

    @Column(name = "submitted_by")
    private Long submittedBy;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "review_note")
    private String reviewNote;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
