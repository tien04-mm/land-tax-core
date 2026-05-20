package com.thanglong.landtax.domain.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandPriceEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxExemptSubjectEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandPriceJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxExemptSubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Domain service xu ly logic tinh thue dat.
 *
 * <p><b>Cong thuc tinh thue:</b></p>
 * <pre>
 *   Tien thue = Dien tich x Don gia dat x (1 - discountRate / 100)
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class TaxCalculationService {

    private final LandParcelJpaRepository landParcelJpaRepository;
    private final LandPriceJpaRepository landPriceJpaRepository;
    private final TaxExemptSubjectRepository taxExemptSubjectRepository;

    /**
     * Tinh thue dat cho mot thua dat cu the.
     * Cong thuc: Tien thue = Dien tich x Don gia dat x (1 - discountRate / 100)
     *
     * @param parcelId  ID thua dat trong bang land_parcels
     * @param citizenId ID cua cong dan
     * @param year      Nam tinh thue
     * @return Ket qua tinh thue bao gom so tien, don gia
     * @throws RuntimeException neu khong tim thay thua dat, don gia
     */
    public TaxCalculationResult calculateTax(Integer parcelId, Integer citizenId, Integer year) {
        // ===== 1. Tim thong tin thua dat =====
        LandParcelEntity parcel = landParcelJpaRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Thua dat khong ton tai: " + parcelId));

        BigDecimal actualArea = parcel.getAreaSize();

        // ===== 2. Tim don gia dat theo loai dat + khu vuc =====
        LandPriceEntity landPrice = landPriceJpaRepository
                .findLatestPrice(parcel.getLandTypeId(), parcel.getAreaId())
                .orElseThrow(() -> new RuntimeException(
                        String.format("Khong tim thay don gia dat cho landTypeId=%d, areaId=%d",
                                parcel.getLandTypeId(), parcel.getAreaId())));

        BigDecimal unitPrice = landPrice.getUnitPrice();

        // ===== 3. Kiem tra mien giam thue (TaxExemptSubject) =====
        BigDecimal exemptionRate = BigDecimal.ZERO;
        if (citizenId != null) {
            Optional<TaxExemptSubjectEntity> exemptOpt = taxExemptSubjectRepository
                    .findByCitizenIdAndAppliedYear(citizenId, year);
            if (exemptOpt.isPresent()) {
                exemptionRate = exemptOpt.get().getDiscountRate();
                log.info("Citizen {} is exempt for year {} with discount rate {}%", citizenId, year, exemptionRate);
            }
        }

        // ===== 4. Tinh tien thue =====
        // multiplier = 1 - (exemptionRate / 100)
        BigDecimal multiplier = BigDecimal.ONE.subtract(
                exemptionRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
        );

        BigDecimal taxAmount = actualArea
                .multiply(unitPrice)
                .multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Tax calculated for parcel {}: area={} m2, unitPrice={} VND/m2, " +
                        "exemptionRate={}% (multiplier={}), tax={} VND, year={}",
                parcelId, actualArea, unitPrice, exemptionRate, multiplier, taxAmount, year);

        return TaxCalculationResult.builder()
                .taxAmount(taxAmount)
                .unitPrice(unitPrice)
                .exemptionRate(exemptionRate)
                .actualArea(actualArea)
                .build();
    }

    // ===== Inner classes cho ket qua =====

    @lombok.Builder
    @lombok.Getter
    public static class TaxCalculationResult {
        private final BigDecimal taxAmount;
        private final BigDecimal unitPrice;
        private final BigDecimal exemptionRate;
        private final BigDecimal actualArea;
    }
}
