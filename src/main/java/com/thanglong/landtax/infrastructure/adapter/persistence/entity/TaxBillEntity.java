package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tax_bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxBillEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_id")
    private Integer billId;

    @Column(name = "cccd_number", length = 20)
    private String cccdNumber;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "declaration_id")
    private Integer declarationId;

    @Column(name = "calculation_formula")
    private String calculationFormula;

    @Column(name = "base_price", precision = 18, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "coefficient", precision = 5, scale = 2)
    private BigDecimal coefficient;
}
