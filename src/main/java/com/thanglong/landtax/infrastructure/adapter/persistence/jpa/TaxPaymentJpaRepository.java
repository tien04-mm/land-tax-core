package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxPaymentJpaRepository extends JpaRepository<TaxPaymentEntity, Integer> {

    List<TaxPaymentEntity> findByLandParcelId(Integer landParcelId);

    List<TaxPaymentEntity> findByRecordId(Integer recordId);

    List<TaxPaymentEntity> findByPaymentStatus(String paymentStatus);

    Optional<TaxPaymentEntity> findByTransactionCode(String transactionCode);

    Optional<TaxPaymentEntity> findByLandParcelIdAndTaxYear(Integer landParcelId, Integer taxYear);
}
