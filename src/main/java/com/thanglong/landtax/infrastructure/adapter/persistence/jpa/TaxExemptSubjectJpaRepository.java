package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxExemptSubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxExemptSubjectJpaRepository extends JpaRepository<TaxExemptSubjectEntity, Integer> {
    
    List<TaxExemptSubjectEntity> findByCitizenId(Integer citizenId);
    
    List<TaxExemptSubjectEntity> findByAppliedYear(Integer appliedYear);
}
