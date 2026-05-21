package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ReconciliationBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReconciliationBatchJpaRepository extends JpaRepository<ReconciliationBatchEntity, Integer> {
}
