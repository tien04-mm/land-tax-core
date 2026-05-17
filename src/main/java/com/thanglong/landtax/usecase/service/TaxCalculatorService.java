package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.controller.exception.ResourceNotFoundException;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandPriceEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandPriceJpaRepository;
import com.thanglong.landtax.usecase.dto.CalculateTaxRequest;
import com.thanglong.landtax.usecase.dto.CalculateTaxResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxCalculatorService {

    private final LandPriceJpaRepository landPriceJpaRepository;

    @Transactional(readOnly = true)
    public CalculateTaxResponse calculateEstimatedTax(CalculateTaxRequest request) {
        log.info("Calculating tax: areaId={}, landTypeId={}, declaredArea={}", 
                request.getAreaId(), request.getLandTypeId(), request.getDeclaredArea());

        if (request.getAreaId() == null || request.getLandTypeId() == null || request.getDeclaredArea() == null) {
            throw new IllegalArgumentException("Các trường areaId, landTypeId, declaredArea không được để trống");
        }

        LandPriceEntity landPrice = landPriceJpaRepository.findLatestPrice(request.getLandTypeId(), request.getAreaId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn giá cho khu vực và loại đất này"));

        double unitPrice = landPrice.getUnitPrice().doubleValue();
        double taxRate = 0.0003; // 0.03%
        double calculatedAmount = request.getDeclaredArea() * unitPrice * taxRate;

        log.info("Tax calculation completed: unitPrice={}, calculatedAmount={}", unitPrice, calculatedAmount);

        return CalculateTaxResponse.builder()
                .unitPrice(unitPrice)
                .taxRate(taxRate)
                .calculatedAmount(calculatedAmount)
                .build();
    }
}
