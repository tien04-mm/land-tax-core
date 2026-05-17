package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.controller.exception.ResourceNotFoundException;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandOwnerHistoryEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandOwnerHistoryJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository;
import com.thanglong.landtax.usecase.dto.HistoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LandHistoryService {

    private final LandParcelJpaRepository landParcelJpaRepository;
    private final LandOwnerHistoryJpaRepository landOwnerHistoryJpaRepository;

    @Transactional(readOnly = true)
    public List<HistoryDTO> getLandHistory(Integer parcelId) {
        log.info("Fetching history for land parcel ID: {}", parcelId);

        // Check if land parcel exists
        if (!landParcelJpaRepository.existsById(parcelId)) {
            throw new ResourceNotFoundException("Thửa đất không tồn tại với ID: " + parcelId);
        }

        List<LandOwnerHistoryEntity> historyEntities = landOwnerHistoryJpaRepository.findByParcelIdOrderByEffectiveDateDesc(parcelId);
        log.info("Found {} history records for land parcel ID: {}", historyEntities.size(), parcelId);

        return historyEntities.stream()
                .map(entity -> HistoryDTO.builder()
                        .id(entity.getHistoryId())
                        .parcelId(entity.getParcelId())
                        .oldOwnerCccd(entity.getOldOwnerCccd())
                        .newOwnerCccd(entity.getNewOwnerCccd())
                        .mutationDate(entity.getEffectiveDate())
                        .status("APPROVED")
                        .note(entity.getNote())
                        .build())
                .collect(Collectors.toList());
    }
}
