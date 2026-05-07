package com.thanglong.landtax.infrastructure.adapter.persistence;

import com.thanglong.landtax.domain.model.TaxRecord;
import com.thanglong.landtax.domain.repository.TaxRecordRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxRecordEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxRecordJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Persistence Adapter — cầu nối giữa domain interface {@link TaxRecordRepository}
 * và Spring Data JPA {@link TaxRecordJpaRepository}.
 *
 * <p>Tuân thủ kiến trúc Hexagonal: domain layer không phụ thuộc vào JPA.
 * Spring Boot sẽ inject bean này khi bất kỳ use case nào yêu cầu
 * {@code TaxRecordRepository}.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class TaxRecordPersistenceAdapter implements TaxRecordRepository {

    private final TaxRecordJpaRepository taxRecordJpaRepository;

    // ===== Mapping helpers =====

    private TaxRecord toDomain(TaxRecordEntity entity) {
        if (entity == null) return null;
        return TaxRecord.builder()
                .id(entity.getId())
                .landParcelId(entity.getLandParcelId())
                .citizenId(entity.getCitizenId())
                .taxYear(entity.getTaxYear())
                .taxableArea(entity.getTaxableArea())
                .taxRate(entity.getTaxRate())
                .taxAmount(entity.getTaxAmount())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .declarationDate(entity.getDeclarationDate())
                .approvalDate(entity.getApprovalDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TaxRecordEntity toEntity(TaxRecord domain) {
        if (domain == null) return null;
        return TaxRecordEntity.builder()
                .id(domain.getId())
                .landParcelId(domain.getLandParcelId())
                .citizenId(domain.getCitizenId())
                .taxYear(domain.getTaxYear())
                .taxableArea(domain.getTaxableArea())
                .taxRate(domain.getTaxRate())
                .taxAmount(domain.getTaxAmount())
                .status(domain.getStatus())
                .notes(domain.getNotes())
                .declarationDate(domain.getDeclarationDate())
                .approvalDate(domain.getApprovalDate())
                .build();
    }

    // ===== TaxRecordRepository contract =====

    @Override
    public Optional<TaxRecord> findById(Long id) {
        return taxRecordJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<TaxRecord> findByCitizenId(Long citizenId) {
        return taxRecordJpaRepository.findByCitizenId(citizenId)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<TaxRecord> findByLandParcelId(Long landParcelId) {
        return taxRecordJpaRepository.findByLandParcelId(landParcelId)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<TaxRecord> findByStatus(String status) {
        return taxRecordJpaRepository.findByStatus(status)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<TaxRecord> findAll() {
        return taxRecordJpaRepository.findAll()
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public TaxRecord save(TaxRecord taxRecord) {
        TaxRecordEntity entity = toEntity(taxRecord);
        TaxRecordEntity saved = taxRecordJpaRepository.save(entity);
        log.debug("TaxRecord saved: id={}, status={}", saved.getId(), saved.getStatus());
        return toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        taxRecordJpaRepository.deleteById(id);
        log.debug("TaxRecord deleted: id={}", id);
    }
}
