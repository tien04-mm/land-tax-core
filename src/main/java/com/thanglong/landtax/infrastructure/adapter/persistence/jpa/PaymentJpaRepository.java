package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByTransactionId(String transactionId);

    List<PaymentEntity> findByCitizenId(Long citizenId);

    List<PaymentEntity> findByTaxRecordId(Long taxRecordId);

    List<PaymentEntity> findByStatus(String status);
}
