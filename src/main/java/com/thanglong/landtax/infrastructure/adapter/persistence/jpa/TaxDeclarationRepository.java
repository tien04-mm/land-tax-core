package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxDeclarationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxDeclarationRepository extends JpaRepository<TaxDeclarationEntity, Integer> {
    List<TaxDeclarationEntity> findBySenderCccdOrderBySubmittedAtDesc(String senderCccd);
    List<TaxDeclarationEntity> findByCitizenIdAndParcelIdAndStatus(Integer citizenId, Integer parcelId, String status);
    long countByStatus(String status);
}
