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
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository citizenLocalJpaRepository;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandOwnerJpaRepository landOwnerJpaRepository;

    public List<LandParcelDTO> getMyLandParcels(String cccd) {
        log.info("Fetching land parcels for CCCD: {}", cccd);
        
        Integer citizenId = citizenLocalJpaRepository.findByCccdNumber(cccd)
                .map(com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity::getCitizenId)
                .orElse(null);
                
        if (citizenId == null) {
            log.warn("Citizen not found for CCCD: {}", cccd);
            System.out.println(">>> [DEBUG] Citizen not found for CCCD: " + cccd);
            return List.of();
        }
        
        System.out.println(">>> [DEBUG] Found citizenId: " + citizenId + " for CCCD: " + cccd);
        
        List<com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandOwnerEntity> ownerships = landOwnerJpaRepository.findByCitizenId(citizenId);
        System.out.println(">>> [DEBUG] Found " + ownerships.size() + " ownership records in land_owners for citizenId: " + citizenId);

        List<Integer> parcelIds = ownerships.stream()
                .map(com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandOwnerEntity::getLandParcelId)
                .collect(Collectors.toList());
        
        System.out.println(">>> [DEBUG] Parcel IDs extracted from land_owners: " + parcelIds);

        List<LandParcelEntity> parcels = landParcelJpaRepository.findAllById(parcelIds);
        
        System.out.println(">>> [DEBUG] Found " + parcels.size() + " actual parcels in land_parcels table");
        
        if (parcels.isEmpty()) {
            log.warn("No land parcels found for CCCD: {}", cccd);
        } else {
            log.info("Found {} land parcels for CCCD: {}", parcels.size(), cccd);
        }
        
        return parcels.stream()
                .map(p -> convertToDTO(p, cccd))
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
            if (updatedEntity.getAttachedHouse() != null) {
                existing.setAttachedHouse(updatedEntity.getAttachedHouse());
            }
            if (updatedEntity.getAttachedOther() != null) {
                existing.setAttachedOther(updatedEntity.getAttachedOther());
            }
            return landParcelJpaRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Land parcel not found with ID " + id));
    }
    
    @Transactional
    public void deleteParcel(Integer id) {
        landParcelJpaRepository.deleteById(id);
    }

    public boolean isOwner(Integer parcelId, String cccd) {
        Integer citizenId = citizenLocalJpaRepository.findByCccdNumber(cccd)
                .map(com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity::getCitizenId)
                .orElse(null);
        if (citizenId == null) return false;
        
        return landOwnerJpaRepository.findByLandParcelId(parcelId).stream()
                .anyMatch(o -> o.getCitizenId().equals(citizenId));
    }

    private LandParcelDTO convertToDTO(LandParcelEntity entity, String ownerCccd) {
        if (entity == null) {
            return null;
        }
        
        System.out.println(">>> [DEBUG] Entity tr  c khi map: " + entity.toString());
        
        LandParcelDTO dto = LandParcelDTO.builder()
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
                .notes(entity.getNotes())
                .attachedHouse(entity.getAttachedHouse())
                .attachedOther(entity.getAttachedOther())
                .ownerCccd(ownerCccd)
                .build();
                
        System.out.println(">>> [DEBUG MAPPER] Parcel DTO AFTER Mapping: " + dto.toString());
        
        return dto;
    }
}
