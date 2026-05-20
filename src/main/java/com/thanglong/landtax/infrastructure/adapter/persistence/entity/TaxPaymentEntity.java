package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity mapping voi bang tax_payments trong land_tax_management.sql.
 */
@Entity
@Table(name = "tax_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxPaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pay_id")
    private Integer payId;

    @Column(name = "record_id")
    private Integer recordId;                   // FK  records.record_id (nullable)

    @Column(name = "land_parcel_id", nullable = false)
    private Integer landParcelId;               // FK  land_parcels.land_parcel_id

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Column(name = "total_amount_due", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmountDue;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "late_fee_amount", precision = 18, scale = 2)
    private BigDecimal lateFeeAmount;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus;               // UNPAID, PAID, OVERDUE

    @Column(name = "transaction_code", length = 100, unique = true)
    private String transactionCode;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
