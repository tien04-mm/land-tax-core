package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA Entity mapping voi bang land_owners trong CSDL.
 * Luu quan he N-N giua cong dan (citizen) va thua dat (land_parcel).
 */
@Entity
@Table(name = "land_owners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandOwnerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ownership_id")
    private Integer ownershipId;

    @Column(name = "citizen_id", nullable = false)
    private Integer citizenId;

    @Column(name = "land_parcel_id", nullable = false)
    private Integer landParcelId;

    @Column(name = "ownership_type", length = 50)
    private String ownershipType; // PRIMARY, CO_OWNER, INHERITED
}
