package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LandParcelService {

    private final LandParcelJpaRepository landParcelJpaRepository;

    public List<LandParcelEntity> getMyLandParcels(String cccd) {
        log.info("Fetching land parcels for CCCD: {}", cccd);
        List<LandParcelEntity> parcels = landParcelJpaRepository.findByOwnerCccd(cccd);
        
        if (parcels.isEmpty()) {
            log.warn("No land parcels found for CCCD: {}", cccd);
        } else {
            log.info("Found {} land parcels for CCCD: {}", parcels.size(), cccd);
        }
        
        return parcels;
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
}
