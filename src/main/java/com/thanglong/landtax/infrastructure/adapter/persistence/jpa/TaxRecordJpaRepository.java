package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxRecordJpaRepository extends JpaRepository<TaxRecordEntity, Long> {

    List<TaxRecordEntity> findByCitizenId(Long citizenId);

    List<TaxRecordEntity> findByLandParcelId(Long landParcelId);

    List<TaxRecordEntity> findByStatus(String status);

    List<TaxRecordEntity> findByTaxYear(Integer taxYear);
}
