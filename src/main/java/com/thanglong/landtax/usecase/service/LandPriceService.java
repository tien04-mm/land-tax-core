package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandPriceJpaRepository;
import com.thanglong.landtax.usecase.dto.LandPriceHistoryResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LandPriceService {

    private final LandPriceJpaRepository landPriceJpaRepository;

    /**
     * Lấy lịch sử điều chỉnh giá đất, sắp xếp theo ngày áp dụng giảm dần.
     */
    public List<LandPriceHistoryResponseDTO> getPriceHistory(Integer landTypeId, Integer areaId) {
        log.info("Fetching land price history for landTypeId={}, areaId={}", landTypeId, areaId);
        return landPriceJpaRepository.findPriceHistory(landTypeId, areaId).stream()
                .map(entity -> LandPriceHistoryResponseDTO.builder()
                        .priceId(entity.getPriceId())
                        .landTypeId(entity.getLandTypeId())
                        .areaId(entity.getAreaId())
                        .unitPrice(entity.getUnitPrice())
                        .appliedFrom(entity.getAppliedFrom())
                        .build())
                .collect(Collectors.toList());
    }
}
