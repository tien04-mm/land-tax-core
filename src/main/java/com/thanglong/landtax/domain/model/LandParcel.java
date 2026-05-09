package com.thanglong.landtax.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model dai dien cho thua dat.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandParcel {

    private Long id;
    private String parcelCode;          // Ma thua dat
    private String address;
    private BigDecimal area;            // Dien tich (m)
    private String landType;            // Loai dat
    private String purpose;             // Muc dich su dung
    private Long ownerId;              // ID chu so huu
    private String ownerName;
    private BigDecimal landPrice;       // Gia dat (VND/m)
    private String status;             // ACTIVE, PENDING, DISPUTED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
