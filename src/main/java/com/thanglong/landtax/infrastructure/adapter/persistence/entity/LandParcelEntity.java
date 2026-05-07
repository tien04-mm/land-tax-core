package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * JPA Entity mapping chính xác với bảng land_parcels trong land_tax_management.sql.
 */
@Entity
@Table(name = "land_parcels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandParcelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "land_parcel_id")
    private Integer landParcelId;

    @Column(name = "land_type_id", nullable = false)
    private Integer landTypeId;                 // FK → land_types.land_type_id

    @Column(name = "area_id", nullable = false)
    private Integer areaId;                     // FK → areas.area_id

    @Column(name = "parcel_number", nullable = false, length = 50)
    private String parcelNumber;

    @Column(name = "map_sheet_number", nullable = false, length = 50)
    private String mapSheetNumber;

    @Column(name = "area_size", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaSize;                // Diện tích thực tế (m²)

    @Column(name = "usage_duration", length = 100)
    private String usageDuration;

    @Column(name = "usage_type", length = 100)
    private String usageType;

    @Column(name = "usage_origin", columnDefinition = "TEXT")
    private String usageOrigin;

    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "certificate_number", length = 50)
    private String certificateNumber;

    @Column(name = "gcn_book_number", length = 50)
    private String gcnBookNumber;

    @Column(name = "attached_house", columnDefinition = "TEXT")
    private String attachedHouse;

    @Column(name = "attached_other", columnDefinition = "TEXT")
    private String attachedOther;

    @Column(name = "land_info_pdf", length = 255)
    private String landInfoPdf;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "owner_cccd", length = 20)
    private String ownerCccd;
}
