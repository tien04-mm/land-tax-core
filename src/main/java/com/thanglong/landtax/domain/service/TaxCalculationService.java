package com.thanglong.landtax.domain.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandPriceEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxExemptSubjectEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandPriceJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxExemptSubjectJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Domain service xử lý logic tính toán thuế đất.
 *
 * <p><b>Công thức tính thuế:</b></p>
 * <pre>
 *   Tiền thuế = Diện tích (m²) × Đơn giá đất (VNĐ/m²) × Thuế suất
 * </pre>
 *
 * <p><b>Tra cứu dữ liệu:</b></p>
 * <ul>
 *   <li>Đơn giá đất: từ bảng land_prices dựa trên land_type_id + area_id của thửa đất</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class TaxCalculationService {

    private final LandParcelJpaRepository landParcelJpaRepository;
    private final LandPriceJpaRepository landPriceJpaRepository;

    private final TaxExemptSubjectJpaRepository taxExemptSubjectJpaRepository;

    /**
     * Tính thuế đất cho một thửa đất cụ thể.
     *
     * @param parcelId  ID thửa đất trong bảng land_parcels
     * @param citizenId ID của công dân để kiểm tra miễn giảm
     * @param year      Năm tính thuế
     * @return Kết quả tính thuế bao gồm số tiền, đơn giá, mức miễn giảm
     * @throws RuntimeException nếu không tìm thấy thửa đất, đơn giá
     */
    public TaxCalculationResult calculateTax(Integer parcelId, Integer citizenId, Integer year) {
        // ===== 1. Tìm thông tin thửa đất =====
        LandParcelEntity parcel = landParcelJpaRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Thửa đất không tồn tại: " + parcelId));

        BigDecimal actualArea = parcel.getAreaSize();

        // ===== 2. Tìm đơn giá đất theo loại đất + khu vực =====
        LandPriceEntity landPrice = landPriceJpaRepository
                .findLatestPrice(parcel.getLandTypeId(), parcel.getAreaId())
                .orElseThrow(() -> new RuntimeException(
                        String.format("Không tìm thấy đơn giá đất cho landTypeId=%d, areaId=%d",
                                parcel.getLandTypeId(), parcel.getAreaId())));

        // ===== 3. Kiểm tra miễn giảm =====
        BigDecimal exemptionRate = BigDecimal.ZERO;
        java.util.List<TaxExemptSubjectEntity> exemptions = 
                taxExemptSubjectJpaRepository.findByCitizenId(citizenId);
        
        if (!exemptions.isEmpty()) {
            // Lấy mức miễn giảm cao nhất nếu có nhiều record
            exemptionRate = exemptions.stream()
                    .map(TaxExemptSubjectEntity::getDiscountRate)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }

        // ===== 4. Tính tiền thuế =====
        // Tiền thuế = Diện tích × Đơn giá × (1 - Exemption Rate)
        BigDecimal unitPrice = landPrice.getUnitPrice();
        
        BigDecimal rateMultiplier = BigDecimal.ONE.subtract(exemptionRate);
        if (rateMultiplier.compareTo(BigDecimal.ZERO) < 0) rateMultiplier = BigDecimal.ZERO; // Không thể âm

        BigDecimal taxAmount = actualArea
                .multiply(unitPrice)
                .multiply(rateMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Tax calculated for parcel {}: area={} m², unitPrice={} VNĐ/m², " +
                        "exemptionRate={}, tax={} VNĐ, year={}",
                parcelId, actualArea, unitPrice, exemptionRate, taxAmount, year);

        return TaxCalculationResult.builder()
                .taxAmount(taxAmount)
                .unitPrice(unitPrice)
                .exemptionRate(exemptionRate)
                .actualArea(actualArea)
                .build();
    }

    // ===== Inner classes cho kết quả =====

    @lombok.Builder
    @lombok.Getter
    public static class TaxCalculationResult {
        private final BigDecimal taxAmount;
        private final BigDecimal unitPrice;
        private final BigDecimal exemptionRate;
        private final BigDecimal actualArea;
    }
}
