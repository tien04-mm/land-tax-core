package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * JPA Entity mapping với bảng areas trong land_tax_management.sql.
 */
@Entity
@Table(name = "areas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AreaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "area_id")
    private Integer areaId;

    @Column(name = "district_code", nullable = false, length = 20)
    private String districtCode;

    @Column(name = "ward_code", nullable = false, length = 20)
    private String wardCode;

    @Column(name = "street_name", length = 255)
    private String streetName;

    @Column(name = "position_level", nullable = false)
    private Integer positionLevel;

    @Column(name = "land_quota", precision = 10, scale = 2)
    private BigDecimal landQuota;
}
