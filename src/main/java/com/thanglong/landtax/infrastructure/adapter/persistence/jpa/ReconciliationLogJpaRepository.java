package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ReconciliationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconciliationLogJpaRepository extends JpaRepository<ReconciliationLogEntity, Integer> {

    List<ReconciliationLogEntity> findByStatusOrderByCreatedAtDesc(String status);

    List<ReconciliationLogEntity> findByTransactionCode(String transactionCode);
}
