package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaxRateJpaRepository extends JpaRepository<TaxRateEntity, Integer> {

    Optional<TaxRateEntity> findByRateCode(String rateCode);
}
