package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ReconciliationStatementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReconciliationStatementRepository extends JpaRepository<ReconciliationStatementEntity, Integer> {
    Optional<ReconciliationStatementEntity> findByBankTransactionRef(String bankTransactionRef);
    List<ReconciliationStatementEntity> findByMatchStatus(String matchStatus);
    List<ReconciliationStatementEntity> findBySourceFile(String sourceFile);
}
