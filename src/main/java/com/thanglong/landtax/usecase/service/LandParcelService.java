package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository;
import com.thanglong.landtax.usecase.dto.LandParcelDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class LandParcelService {

    private final LandParcelJpaRepository landParcelJpaRepository;

    public List<LandParcelDTO> getMyLandParcels(String cccd) {
        log.info("Fetching land parcels for CCCD: {}", cccd);
        List<LandParcelEntity> parcels = landParcelJpaRepository.findByOwnerCccd(cccd);
        
        if (parcels.isEmpty()) {
            log.warn("No land parcels found for CCCD: {}", cccd);
        } else {
            log.info("Found {} land parcels for CCCD: {}", parcels.size(), cccd);
        }
        
        return parcels.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<LandParcelEntity> getAllParcels() {
        return landParcelJpaRepository.findAll();
    }

    public Optional<LandParcelEntity> getParcelById(Integer id) {
        return landParcelJpaRepository.findById(id);
    }

    @Transactional
    public LandParcelEntity createParcel(LandParcelEntity entity) {
        log.info("Creating new land parcel: {}", entity.getParcelNumber());
        return landParcelJpaRepository.save(entity);
    }

    @Transactional
    public LandParcelEntity updateParcel(Integer id, LandParcelEntity updatedEntity) {
        if (updatedEntity == null) {
            throw new IllegalArgumentException("Updated land parcel data cannot be null");
        }
        log.info("Updating land parcel ID: {}", id);
        return landParcelJpaRepository.findById(id).map(existing -> {
            if (updatedEntity.getAddress() != null) {
                existing.setAddress(updatedEntity.getAddress());
            }
            if (updatedEntity.getOwnerCccd() != null) {
                existing.setOwnerCccd(updatedEntity.getOwnerCccd());
            }
            if (updatedEntity.getAreaSize() != null) {
                existing.setAreaSize(updatedEntity.getAreaSize());
            }
            if (updatedEntity.getLandTypeId() != null) {
                existing.setLandTypeId(updatedEntity.getLandTypeId());
            }
            if (updatedEntity.getAreaId() != null) {
                existing.setAreaId(updatedEntity.getAreaId());
            }
            if (updatedEntity.getMapSheetNumber() != null) {
                existing.setMapSheetNumber(updatedEntity.getMapSheetNumber());
            }
            return landParcelJpaRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Land parcel not found with ID " + id));
    }
    
    @Transactional
    public void deleteParcel(Integer id) {
        landParcelJpaRepository.deleteById(id);
    }

    private LandParcelDTO convertToDTO(LandParcelEntity entity) {
        if (entity == null) {
            return null;
        }
        return LandParcelDTO.builder()
                .landParcelId(entity.getLandParcelId())
                .landTypeId(entity.getLandTypeId())
                .areaId(entity.getAreaId())
                .parcelNumber(entity.getParcelNumber())
                .mapSheetNumber(entity.getMapSheetNumber())
                .areaSize(entity.getAreaSize())
                .usageDuration(entity.getUsageDuration())
                .usageType(entity.getUsageType())
                .usageOrigin(entity.getUsageOrigin())
                .address(entity.getAddress())
                .certificateNumber(entity.getCertificateNumber())
                .gcnBookNumber(entity.getGcnBookNumber())
                .attachedHouse(entity.getAttachedHouse())
                .attachedOther(entity.getAttachedOther())
                .landInfoPdf(entity.getLandInfoPdf())
                .notes(entity.getNotes())
                .ownerCccd(entity.getOwnerCccd())
                .build();
    }
}
