package com.thanglong.landtax.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model đại diện cho bản ghi thuế đất.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxRecord {

    private Long id;
    private Long landParcelId;
    private Long citizenId;
    private Integer taxYear;
    private BigDecimal taxableArea;      // Diện tích chịu thuế
    private BigDecimal taxRate;          // Thuế suất (%)
    private BigDecimal taxAmount;        // Số tiền thuế
    private String status;              // PENDING, APPROVED, PAID, OVERDUE
    private String notes;
    private LocalDateTime declarationDate;
    private LocalDateTime approvalDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
