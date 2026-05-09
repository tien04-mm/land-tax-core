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
 * Domain service xo ly logic tAnh toan thua dat.
 *
 * <p><b>CAng thoc tAnh thua:</b></p>
 * <pre>
 *   Tion thua = Dien tAch (mo) A on gia dat (VN/mo) A Thua suat
 * </pre>
 *
 * <p><b>Tra cou du lieu:</b></p>
 * <ul>
 *   <li>on gia dat: tu bang land_prices doa trAn land_type_id + area_id coa thoa dat</li>
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
     * TAnh thua dat cho mot thoa dat co the.
     *
     * @param parcelId  ID thoa dat trong bang land_parcels
     * @param citizenId ID coa cAng dan de kiem tra mien giam
     * @param year      Nm tAnh thua
     * @return Kat qua tAnh thua bao gom so tion, don gia, moc mien giam
     * @throws RuntimeException nau khAng tim thay thoa dat, don gia
     */
    public TaxCalculationResult calculateTax(Integer parcelId, Integer citizenId, Integer year) {
        // ===== 1. Tim thAng tin thoa dat =====
        LandParcelEntity parcel = landParcelJpaRepository.findById(parcelId)
                .orElseThrow(() -> new RuntimeException("Thoa dat khAng ton tai: " + parcelId));

        BigDecimal actualArea = parcel.getAreaSize();

        // ===== 2. Tim don gia dat theo loai dat + khu voc =====
        LandPriceEntity landPrice = landPriceJpaRepository
                .findLatestPrice(parcel.getLandTypeId(), parcel.getAreaId())
                .orElseThrow(() -> new RuntimeException(
                        String.format("KhAng tim thay don gia dat cho landTypeId=%d, areaId=%d",
                                parcel.getLandTypeId(), parcel.getAreaId())));

        // ===== 3. Kiem tra mien giam =====
        BigDecimal exemptionRate = BigDecimal.ZERO;
        java.util.List<TaxExemptSubjectEntity> exemptions = 
                taxExemptSubjectJpaRepository.findByCitizenId(citizenId);
        
        if (!exemptions.isEmpty()) {
            // Lay moc mien giam cao nhat nau co nhiou record
            exemptionRate = exemptions.stream()
                    .map(TaxExemptSubjectEntity::getDiscountRate)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }

        // ===== 4. TAnh tion thua =====
        // Tion thua = Dien tAch A on gia A (1 - Exemption Rate)
        BigDecimal unitPrice = landPrice.getUnitPrice();
        
        BigDecimal rateMultiplier = BigDecimal.ONE.subtract(exemptionRate);
        if (rateMultiplier.compareTo(BigDecimal.ZERO) < 0) rateMultiplier = BigDecimal.ZERO; // KhAng the am

        BigDecimal taxAmount = actualArea
                .multiply(unitPrice)
                .multiply(rateMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Tax calculated for parcel {}: area={} mo, unitPrice={} VN/mo, " +
                        "exemptionRate={}, tax={} VN, year={}",
                parcelId, actualArea, unitPrice, exemptionRate, taxAmount, year);

        return TaxCalculationResult.builder()
                .taxAmount(taxAmount)
                .unitPrice(unitPrice)
                .exemptionRate(exemptionRate)
                .actualArea(actualArea)
                .build();
    }

    // ===== Inner classes cho kat qua =====

    @lombok.Builder
    @lombok.Getter
    public static class TaxCalculationResult {
        private final BigDecimal taxAmount;
        private final BigDecimal unitPrice;
        private final BigDecimal exemptionRate;
        private final BigDecimal actualArea;
    }
}

