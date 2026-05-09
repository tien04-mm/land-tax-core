package com.thanglong.landtax.domain.repository;

import com.thanglong.landtax.domain.model.Record;

import java.util.List;
import java.util.Optional;

/**
 * Interface repository cho Record (Ho so/Bien ban).
 */
public interface RecordRepository {

    Optional<Record> findById(Long id);

    List<Record> findByCitizenId(Long citizenId);

    List<Record> findByLandParcelId(Long landParcelId);

    List<Record> findByStatus(String status);

    Record save(Record record);

    void deleteById(Long id);
}
