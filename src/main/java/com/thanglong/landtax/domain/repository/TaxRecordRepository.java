package com.thanglong.landtax.domain.repository;

import com.thanglong.landtax.domain.model.TaxRecord;

import java.util.List;
import java.util.Optional;

/**
 * Interface repository cho TaxRecord.
 */
public interface TaxRecordRepository {

    Optional<TaxRecord> findById(Long id);

    List<TaxRecord> findByCitizenId(Long citizenId);

    List<TaxRecord> findByLandParcelId(Long landParcelId);

    List<TaxRecord> findByStatus(String status);

    List<TaxRecord> findAll();

    TaxRecord save(TaxRecord taxRecord);

    void deleteById(Long id);
}
