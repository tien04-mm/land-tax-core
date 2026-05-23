package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxExemptSubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaxExemptSubjectRepository extends JpaRepository<TaxExemptSubjectEntity, Integer> {
    Optional<TaxExemptSubjectEntity> findByCitizenIdAndAppliedYear(Integer citizenId, Integer appliedYear);

    java.util.List<TaxExemptSubjectEntity> findByCitizenId(Integer citizenId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM TaxExemptSubjectEntity t WHERE " +
           "(:appliedYear IS NULL OR t.appliedYear = :appliedYear) AND " +
           "(:status IS NULL OR t.status = :status) " +
           "ORDER BY t.appliedYear DESC, t.uploadedAt DESC")
    java.util.List<TaxExemptSubjectEntity> filterExemptions(
            @org.springframework.data.repository.query.Param("appliedYear") Integer appliedYear,
            @org.springframework.data.repository.query.Param("status") String status);
}
