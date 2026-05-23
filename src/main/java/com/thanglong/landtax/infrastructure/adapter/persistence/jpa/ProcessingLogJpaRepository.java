package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ProcessingLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessingLogJpaRepository extends JpaRepository<ProcessingLogEntity, Integer> {

    List<ProcessingLogEntity> findByRecordIdOrderByProcessedAtDesc(Integer recordId);

    List<ProcessingLogEntity> findByProcessorAccountId(Integer processorAccountId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM ProcessingLogEntity p WHERE " +
           "(:actionType IS NULL OR p.processingStep = :actionType) AND " +
           "(:actorId IS NULL OR p.processorAccountId = :actorId) " +
           "ORDER BY p.processedAt DESC")
    List<ProcessingLogEntity> filterLogs(
            @org.springframework.data.repository.query.Param("actionType") String actionType,
            @org.springframework.data.repository.query.Param("actorId") Integer actorId);
}
