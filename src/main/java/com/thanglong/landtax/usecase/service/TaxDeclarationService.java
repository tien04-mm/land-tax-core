package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxDeclarationEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxDeclarationRepository;
import com.thanglong.landtax.usecase.dto.TaxDeclarationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class TaxDeclarationService {

    private final TaxDeclarationRepository repository;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository citizenLocalRepository;

    /** Expose repository for direct use by TaxController */
    public TaxDeclarationRepository getRepository() {
        return repository;
    }

    public List<TaxDeclarationResponse> getMyHistory(String cccd) {
        log.info("Fetching history for CCCD: {}", cccd);

        Integer citizenId = getCitizenIdByCccd(cccd);

        List<TaxDeclarationEntity> entities = repository.findByRecordCitizenIdOrderByCreatedAtDesc(citizenId);

        return entities.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public TaxDeclarationResponse getDeclarationById(Integer id, String currentCccd) {
        log.info("Fetching declaration details for record: {} for CCCD: {}", id, currentCccd);

        TaxDeclarationEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tax declaration not found"));

        Integer currentCitizenId = getCitizenIdByCccd(currentCccd);
        if (!java.util.Objects.equals(entity.getRecord().getCitizenId(), currentCitizenId)) {
            log.warn("Security warning: CCCD {} attempted to view declaration {} belonging to citizen {}", currentCccd, id, entity.getRecord().getCitizenId());
            throw new AccessDeniedException("You do not have permission to view this record");
        }

        return mapToResponse(entity);
    }

    private TaxDeclarationResponse mapToResponse(TaxDeclarationEntity entity) {
        return TaxDeclarationResponse.builder()
                .recordId(entity.getRecordId())
                .citizenId(entity.getRecord() != null ? entity.getRecord().getCitizenId() : null)
                .parcelId(entity.getRecord() != null ? entity.getRecord().getLandParcelId() : null)
                .declaredArea(entity.getDeclaredArea())
                .declaredUsage(entity.getDeclaredUsage())
                .status(entity.getRecord() != null ? entity.getRecord().getCurrentStatus() : null)
                .declarationNotes(entity.getDeclarationNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public void cancelDeclaration(Integer id, String cccd) {
        if (id == null || cccd == null) {
            throw new IllegalArgumentException("Invalid input data (null)");
        }

        TaxDeclarationEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tax declaration not found"));

        Integer currentCitizenId = getCitizenIdByCccd(cccd);
        if (!java.util.Objects.equals(currentCitizenId, entity.getRecord().getCitizenId())) {
            throw new RuntimeException("You do not have permission to cancel this declaration");
        }

        if (!"PENDING".equals(entity.getRecord().getCurrentStatus())) {
            throw new IllegalArgumentException("Only PENDING declarations can be cancelled");
        }

        entity.getRecord().setCurrentStatus("CANCELLED");
        repository.save(entity);
        log.info("Cancelled declaration {} for CCCD {}", id, cccd);
    }

    private Integer getCitizenIdByCccd(String cccd) {
        return citizenLocalRepository.findByCccdNumber(cccd)
                .map(com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity::getCitizenId)
                .orElseThrow(() -> new RuntimeException("Citizen not found for CCCD: " + cccd));
    }
}
