package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA Entity mapping với bảng land_prices trong land_tax_management.sql.
 * Bảng giá đất: lưu đơn giá đất (unit_price) theo loại đất + khu vực.
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
    private Integer landTypeId;                 // FK → land_types.land_type_id

    @Column(name = "area_id", nullable = false)
    private Integer areaId;                     // FK → areas.area_id

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;               // Đơn giá đất (VNĐ/m²)

    @Column(name = "applied_from", nullable = false)
    private LocalDate appliedFrom;              // Ngày áp dụng
}
