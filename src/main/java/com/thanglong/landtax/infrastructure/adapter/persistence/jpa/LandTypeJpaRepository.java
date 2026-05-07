package com.thanglong.landtax.infrastructure.adapter.persistence.jpa;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LandTypeJpaRepository extends JpaRepository<LandTypeEntity, Integer> {

    Optional<LandTypeEntity> findByTypeCode(String typeCode);
}
