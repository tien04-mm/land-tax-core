package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordJpaRepository extends JpaRepository<RecordEntity, Integer> {

    List<RecordEntity> findByCitizenId(Integer citizenId);

    List<RecordEntity> findByLandParcelId(Integer landParcelId);

    List<RecordEntity> findByCurrentStatus(String currentStatus);

    List<RecordEntity> findByRecordCategory(String recordCategory);

    List<RecordEntity> findByCitizenIdAndRecordCategory(Integer citizenId, String recordCategory);
}
