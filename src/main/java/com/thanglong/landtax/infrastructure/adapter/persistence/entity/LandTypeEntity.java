package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA Entity mapping voi bang land_types trong land_tax_management.sql.
 */
@Entity
@Table(name = "land_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "land_type_id")
    private Integer landTypeId;

    @Column(name = "type_code", nullable = false, unique = true, length = 10)
    private String typeCode;

    @Column(name = "type_name", nullable = false, length = 150)
    private String typeName;

    @Column(name = "is_tax_payment")
    private Boolean isTaxPayment;
}
