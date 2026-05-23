package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.domain.model.Complaint;
import com.thanglong.landtax.domain.repository.ComplaintRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ComplaintEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Hiện thực hóa ComplaintRepository sử dụng Spring Data JPA và map dữ liệu.
 */
@Repository
@RequiredArgsConstructor
public class ComplaintRepositoryImpl implements ComplaintRepository {

    private final ComplaintJpaRepository complaintJpaRepository;
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;
    private final RecordJpaRepository recordJpaRepository;

    @Override
    public Optional<Complaint> findById(Integer id) {
        return complaintJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Complaint> findByCitizenId(Integer citizenId) {
        return complaintJpaRepository.findByCitizenCitizenId(citizenId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findByComplaintType(String type) {
        return complaintJpaRepository.findByComplaintType(type).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Complaint> findAll() {
        return complaintJpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Complaint save(Complaint complaint) {
        ComplaintEntity entity = toEntity(complaint);
        ComplaintEntity saved = complaintJpaRepository.save(entity);
        return toDomain(saved);
    }

    private Complaint toDomain(ComplaintEntity entity) {
        if (entity == null) {
            return null;
        }
        return Complaint.builder()
                .id(entity.getId())
                .citizenId(entity.getCitizen() != null ? entity.getCitizen().getCitizenId() : null)
                .recordId(entity.getRecord() != null ? entity.getRecord().getRecordId() : null)
                .content(entity.getContent())
                .status(entity.getStatus())
                .responseNote(entity.getResponseNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ComplaintEntity toEntity(Complaint domain) {
        if (domain == null) {
            return null;
        }

        CitizenLocalEntity citizen = null;
        if (domain.getCitizenId() != null) {
            citizen = citizenLocalJpaRepository.findById(domain.getCitizenId()).orElse(null);
        }

        RecordEntity record = null;
        if (domain.getRecordId() != null) {
            record = recordJpaRepository.findById(domain.getRecordId()).orElse(null);
        }

        ComplaintEntity entity = ComplaintEntity.builder()
                .id(domain.getId())
                .content(domain.getContent())
                .status(domain.getStatus())
                .responseNote(domain.getResponseNote())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();

        if (citizen != null) {
            entity.setCitizen(citizen);
        }
        if (record != null) {
            entity.setRecord(record);
        }

        return entity;
    }
}
