package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxDeclarationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxDeclarationRepository extends JpaRepository<TaxDeclarationEntity, Integer> {
    List<TaxDeclarationEntity> findByRecordCitizenIdOrderByCreatedAtDesc(Integer citizenId);
    List<TaxDeclarationEntity> findByRecordCitizenIdAndRecordLandParcelIdAndRecordCurrentStatus(Integer citizenId, Integer landParcelId, String status);
    java.util.Optional<TaxDeclarationEntity> findByRecordId(Integer recordId);
    long countByRecordCurrentStatus(String status);
    java.util.Optional<TaxDeclarationEntity> findByRecordRecordId(Integer recordId);
}
