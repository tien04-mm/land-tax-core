package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LandParcelService {

    private final LandParcelJpaRepository landParcelJpaRepository;

    public List<LandParcelEntity> getMyLandParcels(String cccd) {
        log.info("Bắt đầu truy vấn danh sách thửa đất cho CCCD: {}", cccd);
        List<LandParcelEntity> parcels = landParcelJpaRepository.findByOwnerCccd(cccd);
        
        if (parcels.isEmpty()) {
            log.warn("Không tìm thấy thửa đất nào cho CCCD: {}", cccd);
        } else {
            log.info("Tìm thấy {} thửa đất cho CCCD: {}", parcels.size(), cccd);
        }
        
        return parcels;
    }
}
