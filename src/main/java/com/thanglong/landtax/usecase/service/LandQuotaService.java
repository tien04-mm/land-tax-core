package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.controller.exception.ResourceNotFoundException;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AreaEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AreaJpaRepository;
import com.thanglong.landtax.usecase.dto.LandQuotaResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class LandQuotaService {

    private final AreaJpaRepository areaJpaRepository;
    private final AuditLogService auditLogService;

    /**
     * Cập nhật hạn mức đất ở cho khu vực, lưu log thao tác.
     */
    @Transactional
    public LandQuotaResponseDTO updateLandQuota(Integer areaId, BigDecimal landQuota) {
        log.info("Updating land quota for areaId={} to {}", areaId, landQuota);

        AreaEntity area = areaJpaRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Khu vực không tồn tại với ID " + areaId));

        area.setLandQuota(landQuota);
        AreaEntity saved = areaJpaRepository.save(area);

        auditLogService.log("UPDATE_LAND_QUOTA", "AREA", String.valueOf(areaId),
                "Cập nhật hạn mức đất ở thành " + landQuota);

        return LandQuotaResponseDTO.builder()
                .areaId(saved.getAreaId())
                .districtCode(saved.getDistrictCode())
                .wardCode(saved.getWardCode())
                .streetName(saved.getStreetName())
                .positionLevel(saved.getPositionLevel())
                .landQuota(saved.getLandQuota())
                .build();
    }

    /**
     * Lấy thông tin hạn mức đất ở của một khu vực.
     */
    public LandQuotaResponseDTO getLandQuota(Integer areaId) {
        log.info("Fetching land quota for areaId={}", areaId);
        AreaEntity area = areaJpaRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Khu vực không tồn tại với ID " + areaId));
        return LandQuotaResponseDTO.builder()
                .areaId(area.getAreaId())
                .districtCode(area.getDistrictCode())
                .wardCode(area.getWardCode())
                .streetName(area.getStreetName())
                .positionLevel(area.getPositionLevel())
                .landQuota(area.getLandQuota())
                .build();
    }
}
