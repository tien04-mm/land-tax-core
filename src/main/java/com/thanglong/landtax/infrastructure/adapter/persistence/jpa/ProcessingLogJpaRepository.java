package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ProcessingLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessingLogJpaRepository extends JpaRepository<ProcessingLogEntity, Integer> {

    List<ProcessingLogEntity> findByRecordIdOrderByProcessedAtDesc(Integer recordId);

    List<ProcessingLogEntity> findByProcessorAccountId(Integer processorAccountId);
}
