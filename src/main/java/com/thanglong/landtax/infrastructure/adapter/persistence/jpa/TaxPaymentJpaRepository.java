package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @org.springframework.data.jpa.repository.Query("SELECT SUM(p.totalAmountDue) FROM TaxPaymentEntity p WHERE p.paymentStatus = 'PAID' AND p.taxYear = :year")
    java.math.BigDecimal sumPaidAmountByYear(int year);

    @Query("SELECT MONTH(p.paidAt) as month, SUM(p.totalAmountDue) as totalRevenue " +
           "FROM TaxPaymentEntity p " +
           "WHERE YEAR(p.paidAt) = :year AND p.paymentStatus = 'PAID' " +
           "GROUP BY MONTH(p.paidAt)")
    List<Object[]> getMonthlyRevenue(@Param("year") int year);
}
