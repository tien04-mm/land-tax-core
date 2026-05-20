package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxExemptSubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaxExemptSubjectRepository extends JpaRepository<TaxExemptSubjectEntity, Integer> {
    Optional<TaxExemptSubjectEntity> findByCitizenIdAndAppliedYear(Integer citizenId, Integer appliedYear);
}
