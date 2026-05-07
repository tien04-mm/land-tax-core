package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxBillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxBillRepository extends JpaRepository<TaxBillEntity, Integer> {
    List<TaxBillEntity> findByCccdNumberAndStatus(String cccdNumber, String status);

    List<TaxBillEntity> findByDeclarationId(Integer declarationId);

    List<TaxBillEntity> findByStatus(String status);

    java.util.Optional<TaxBillEntity> findByBillId(Integer billId);
}
