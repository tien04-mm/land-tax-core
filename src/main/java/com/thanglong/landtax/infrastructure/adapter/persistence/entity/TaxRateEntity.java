package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * JPA Entity mapping voi bang tax_rates trong land_tax_management.sql.
 */
@Entity
@Table(name = "tax_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rate_id")
    private Integer rateId;

    @Column(name = "tax_name", nullable = false, length = 100)
    private String taxName;

    @Column(name = "rate_value", nullable = false, precision = 5, scale = 4)
    private BigDecimal rateValue;               // Vi du: 0.0003 cho 0.03%

    @Column(name = "rate_code", nullable = false, unique = true, length = 50)
    private String rateCode;
}
