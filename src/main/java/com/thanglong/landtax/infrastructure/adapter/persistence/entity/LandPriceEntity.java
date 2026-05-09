package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA Entity mapping voi bang land_prices trong land_tax_management.sql.
 * Bang gia dat: luu don gia dat (unit_price) theo loai dat + khu vuc.
 */
@Entity
@Table(name = "land_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandPriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "price_id")
    private Integer priceId;

    @Column(name = "land_type_id", nullable = false)
    private Integer landTypeId;                 // FK  land_types.land_type_id

    @Column(name = "area_id", nullable = false)
    private Integer areaId;                     // FK  areas.area_id

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;               // Don gia dat (VND/m)

    @Column(name = "applied_from", nullable = false)
    private LocalDate appliedFrom;              // Ngay ap dung
}
