package com.thanglong.landtax.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model đại diện cho thửa đất.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandParcel {

    private Long id;
    private String parcelCode;          // Mã thửa đất
    private String address;
    private BigDecimal area;            // Diện tích (m²)
    private String landType;            // Loại đất
    private String purpose;             // Mục đích sử dụng
    private Long ownerId;              // ID chủ sở hữu
    private String ownerName;
    private BigDecimal landPrice;       // Giá đất (VNĐ/m²)
    private String status;             // ACTIVE, PENDING, DISPUTED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
