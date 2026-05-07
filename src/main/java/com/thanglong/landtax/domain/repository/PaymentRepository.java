package com.thanglong.landtax.domain.repository;

import com.thanglong.landtax.domain.model.Payment;

import java.util.List;
import java.util.Optional;

/**
 * Interface repository cho Payment.
 */
public interface PaymentRepository {

    Optional<Payment> findById(Long id);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByCitizenId(Long citizenId);

    List<Payment> findByTaxRecordId(Long taxRecordId);

    Payment save(Payment payment);
}
