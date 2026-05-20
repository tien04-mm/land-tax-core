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

    /** Expose repository for direct use by TaxController */
    public TaxDeclarationRepository getRepository() {
        return repository;
    }

    public List<TaxDeclarationResponse> getMyHistory(String cccd) {
        log.info("Fetching history for CCCD: {}", cccd);

        List<TaxDeclarationEntity> entities = repository.findBySenderCccdOrderBySubmittedAtDesc(cccd);

        return entities.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public TaxDeclarationResponse getDeclarationById(Integer id, String currentCccd) {
        log.info("Fetching declaration details for record: {} for CCCD: {}", id, currentCccd);

        TaxDeclarationEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tax declaration not found"));

        if (!java.util.Objects.equals(entity.getSenderCccd(), currentCccd)) {
            log.warn("Security warning: CCCD {} attempted to view declaration {} belonging to {}", currentCccd, id, entity.getSenderCccd());
            throw new AccessDeniedException("You do not have permission to view this record");
        }

        return mapToResponse(entity);
    }

    private TaxDeclarationResponse mapToResponse(TaxDeclarationEntity entity) {
        return TaxDeclarationResponse.builder()
                .recordId(entity.getRecordId())
                .citizenId(entity.getCitizenId())
                .parcelId(entity.getParcelId())
                .taxYear(entity.getTaxYear())
                .declaredArea(entity.getDeclaredArea())
                .actualArea(entity.getActualArea())
                .declaredPurpose(entity.getDeclaredPurpose())
                .status(entity.getStatus())
                .reviewNote(entity.getReviewNote())
                .calculatedTaxAmount(entity.getCalculatedTaxAmount())
                .unitPrice(entity.getUnitPrice())
                .taxRate(entity.getTaxRate())
                .submittedAt(entity.getSubmittedAt())
                .build();
    }

    public void cancelDeclaration(Integer id, String cccd) {
        if (id == null || cccd == null) {
            throw new IllegalArgumentException("Invalid input data (null)");
        }

        TaxDeclarationEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tax declaration not found"));

        if (!java.util.Objects.equals(cccd, entity.getSenderCccd())) {
            throw new RuntimeException("You do not have permission to cancel this declaration");
        }

        if (!"PENDING".equals(entity.getStatus())) {
            throw new IllegalArgumentException("Only PENDING declarations can be cancelled");
        }

        entity.setStatus("CANCELLED");
        repository.save(entity);
        log.info("Cancelled declaration {} for CCCD {}", id, cccd);
    }
}
